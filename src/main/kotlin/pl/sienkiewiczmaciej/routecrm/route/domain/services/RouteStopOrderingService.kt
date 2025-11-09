// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/services/RouteStopOrderingService.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId

/**
 * Domain service for managing the ordering of route stops.
 * Contains pure business logic for stop reordering algorithms.
 */
@Component
class RouteStopOrderingService {

    /**
     * Inserts new stops at a specific position and shifts existing stops accordingly.
     * All stops at or after the insertion position are shifted by the number of stops being inserted.
     *
     * Example: Existing stops [1,2,3,4], insert 2 stops at position 2
     * Result: [1, (new), (new), 4, 5, 6]
     *
     * @param existingStops Current list of stops (should be sorted by stopOrder)
     * @param insertPosition The position where new stops will be inserted (1-based)
     * @param numberOfStopsToInsert Number of stops that will be inserted
     * @return List of updated stops with new stop orders
     */
    fun insertStopsAt(
        existingStops: List<RouteStop>,
        insertPosition: Int,
        numberOfStopsToInsert: Int
    ): List<RouteStop> {
        require(insertPosition > 0) { "Insert position must be positive" }
        require(numberOfStopsToInsert > 0) { "Number of stops to insert must be positive" }

        return existingStops.map { stop ->
            if (stop.stopOrder >= insertPosition) {
                stop.updateOrder(stop.stopOrder + numberOfStopsToInsert)
            } else {
                stop
            }
        }
    }

    /**
     * Removes specified stops and renumbers the remaining stops to be consecutive starting from 1.
     *
     * Example: Stops [1,2,3,4,5], remove stops at positions 2 and 4
     * Result: [1,2,3] (originally positions 1,3,5)
     *
     * @param existingStops Current list of stops
     * @param stopsToRemove IDs of stops to remove
     * @return List of remaining stops with renumbered stop orders
     */
    fun removeStopsAndReorder(
        existingStops: List<RouteStop>,
        stopsToRemove: Set<RouteStopId>
    ): List<RouteStop> {
        val remainingStops = existingStops.filterNot { it.id in stopsToRemove }

        return remainingStops
            .sortedBy { it.stopOrder }
            .mapIndexed { index, stop ->
                stop.updateOrder(index + 1)
            }
    }

    /**
     * Reorders stops according to a custom mapping of stop IDs to new orders.
     * Validates that the new orders are consecutive starting from 1.
     *
     * @param existingStops Current list of stops
     * @param newOrderMapping Map of RouteStopId to new stop order
     * @return List of stops with updated orders
     * @throws IllegalArgumentException if orders are not consecutive or if not all stops are mapped
     */
    fun reorderStops(
        existingStops: List<RouteStop>,
        newOrderMapping: Map<RouteStopId, Int>
    ): List<RouteStop> {
        // Validate that all stops have a new order
        require(existingStops.all { it.id in newOrderMapping }) {
            "All stops must have a new order defined"
        }

        // Validate that new orders are consecutive starting from 1
        val newOrders = newOrderMapping.values.sorted()
        require(newOrders == (1..newOrders.size).toList()) {
            "New stop orders must be consecutive starting from 1"
        }

        return existingStops.map { stop ->
            val newOrder = newOrderMapping[stop.id]!!
            stop.updateOrder(newOrder)
        }
    }

    /**
     * Calculates the next available stop order for adding a new stop at the end.
     *
     * @param existingStops Current list of stops
     * @return The next available stop order (max + 1, or 1 if no stops exist)
     */
    fun getNextStopOrder(existingStops: List<RouteStop>): Int {
        return if (existingStops.isEmpty()) {
            1
        } else {
            existingStops.maxOf { it.stopOrder } + 1
        }
    }

    /**
     * Validates that stop orders are consecutive starting from 1.
     *
     * @param stops List of stops to validate
     * @return true if orders are valid, false otherwise
     */
    fun areStopOrdersValid(stops: List<RouteStop>): Boolean {
        if (stops.isEmpty()) return true

        val orders = stops.map { it.stopOrder }.sorted()
        return orders == (1..orders.size).toList()
    }
}