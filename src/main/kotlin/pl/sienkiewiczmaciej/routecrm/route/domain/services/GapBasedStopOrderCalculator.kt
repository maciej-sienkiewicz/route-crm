package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop

data class OrderCalculationResult(
    val order: Int,
    val needsRebalancing: Boolean
)

data class MultiOrderCalculationResult(
    val orders: List<Int>,
    val needsRebalancing: Boolean
)

class InsufficientGapException(
    val requiredGap: Int,
    val availableGap: Int
) : RuntimeException("Insufficient gap: required=$requiredGap, available=$availableGap")

@Component
class GapBasedStopOrderCalculator {

    companion object {
        const val GAP_SIZE = 1000
        const val MIN_GAP = 10
        const val REBALANCE_THRESHOLD = 0.7
    }

    fun calculateOrderForInsertion(
        existingStops: List<RouteStop>,
        afterOrder: Int?
    ): OrderCalculationResult {
        if (existingStops.isEmpty()) {
            return OrderCalculationResult(GAP_SIZE, false)
        }

        val sortedStops = existingStops.sortedBy { it.stopOrder }

        if (afterOrder == null) {
            val firstOrder = sortedStops.first().stopOrder
            val newOrder = firstOrder - GAP_SIZE
            return OrderCalculationResult(
                order = maxOf(newOrder, GAP_SIZE),
                needsRebalancing = newOrder < MIN_GAP
            )
        }

        val currentStop = sortedStops.find { it.stopOrder == afterOrder }
            ?: throw IllegalArgumentException("Stop with order $afterOrder not found")

        val currentIndex = sortedStops.indexOf(currentStop)
        val nextStop = sortedStops.getOrNull(currentIndex + 1)

        if (nextStop == null) {
            val newOrder = afterOrder + GAP_SIZE
            return OrderCalculationResult(newOrder, false)
        }

        val gap = nextStop.stopOrder - afterOrder
        if (gap < MIN_GAP) {
            throw InsufficientGapException(MIN_GAP, gap)
        }

        val newOrder = afterOrder + (gap / 2)
        return OrderCalculationResult(newOrder, false)
    }

    fun calculateOrdersForMultipleInsertions(
        existingStops: List<RouteStop>,
        afterOrder: Int?,
        count: Int
    ): MultiOrderCalculationResult {
        require(count > 0) { "Count must be positive" }

        if (existingStops.isEmpty()) {
            val orders = (1..count).map { it * GAP_SIZE }
            return MultiOrderCalculationResult(orders, false)
        }

        val sortedStops = existingStops.sortedBy { it.stopOrder }

        if (afterOrder == null) {
            val firstOrder = sortedStops.first().stopOrder
            val requiredGap = count * MIN_GAP
            val availableGap = firstOrder - MIN_GAP

            if (availableGap < requiredGap) {
                throw InsufficientGapException(requiredGap, availableGap)
            }

            val step = availableGap / (count + 1)
            val orders = (1..count).map { MIN_GAP + (it * step) }
            return MultiOrderCalculationResult(orders, false)
        }

        val currentStop = sortedStops.find { it.stopOrder == afterOrder }
            ?: throw IllegalArgumentException("Stop with order $afterOrder not found")

        val currentIndex = sortedStops.indexOf(currentStop)
        val nextStop = sortedStops.getOrNull(currentIndex + 1)

        if (nextStop == null) {
            val orders = (1..count).map { afterOrder + (it * GAP_SIZE) }
            return MultiOrderCalculationResult(orders, false)
        }

        val gap = nextStop.stopOrder - afterOrder
        val requiredGap = count * MIN_GAP

        if (gap < requiredGap) {
            throw InsufficientGapException(requiredGap, gap)
        }

        val step = gap / (count + 1)
        val orders = (1..count).map { afterOrder + (it * step) }
        return MultiOrderCalculationResult(orders, false)
    }

    fun needsRebalancing(stops: List<RouteStop>): Boolean {
        if (stops.size < 2) return false

        val sortedStops = stops.sortedBy { it.stopOrder }
        val totalRange = sortedStops.last().stopOrder - sortedStops.first().stopOrder
        val idealRange = stops.size * GAP_SIZE

        val compressionRatio = totalRange.toDouble() / idealRange
        return compressionRatio < REBALANCE_THRESHOLD
    }

    fun rebalance(stops: List<RouteStop>): List<RouteStop> {
        if (stops.isEmpty()) return emptyList()

        val sortedStops = stops.sortedBy { it.stopOrder }
        return sortedStops.mapIndexed { index, stop ->
            stop.updateOrder((index + 1) * GAP_SIZE)
        }
    }
}