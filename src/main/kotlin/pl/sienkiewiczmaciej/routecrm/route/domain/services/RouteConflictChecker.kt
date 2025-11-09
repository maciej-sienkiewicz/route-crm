// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/services/RouteConflictChecker.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.TimeRange
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Vehicle
import java.time.LocalDate

/**
 * Result of a conflict check operation.
 */
sealed class ConflictCheckResult {
    object NoConflict : ConflictCheckResult()
    data class HasConflict(val conflictingRoute: Route) : ConflictCheckResult()
}

/**
 * Domain service for checking route scheduling conflicts.
 * Contains pure business logic for detecting time-based conflicts between routes.
 */
@Component
class RouteConflictChecker {

    /**
     * Checks if a driver has a conflicting route at the specified time.
     * Only considers routes that are PLANNED or IN_PROGRESS.
     *
     * @param driver The driver to check
     * @param date The date of the route
     * @param timeRange The time range of the route
     * @param existingRoutes List of existing routes for the driver on the same date
     * @return ConflictCheckResult indicating if there's a conflict
     */
    fun hasDriverConflict(
        driver: Driver,
        date: LocalDate,
        timeRange: TimeRange,
        existingRoutes: List<Route>
    ): ConflictCheckResult {
        return checkConflict(
            timeRange = timeRange,
            existingRoutes = existingRoutes.filter { route ->
                route.driverId == driver.id &&
                        route.date == date &&
                        route.status in listOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS)
            }
        )
    }

    /**
     * Checks if a vehicle has a conflicting route at the specified time.
     * Only considers routes that are PLANNED or IN_PROGRESS.
     *
     * @param vehicle The vehicle to check
     * @param date The date of the route
     * @param timeRange The time range of the route
     * @param existingRoutes List of existing routes for the vehicle on the same date
     * @return ConflictCheckResult indicating if there's a conflict
     */
    fun hasVehicleConflict(
        vehicle: Vehicle,
        date: LocalDate,
        timeRange: TimeRange,
        existingRoutes: List<Route>
    ): ConflictCheckResult {
        return checkConflict(
            timeRange = timeRange,
            existingRoutes = existingRoutes.filter { route ->
                route.vehicleId == vehicle.id &&
                        route.date == date &&
                        route.status in listOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS)
            }
        )
    }

    /**
     * Core conflict detection algorithm.
     * Returns the first conflicting route found, or NoConflict if none found.
     */
    private fun checkConflict(
        timeRange: TimeRange,
        existingRoutes: List<Route>
    ): ConflictCheckResult {
        val conflictingRoute = existingRoutes.firstOrNull { route ->
            val existingTimeRange = TimeRange(route.estimatedStartTime, route.estimatedEndTime)
            timeRange.overlapsWith(existingTimeRange)
        }

        return if (conflictingRoute != null) {
            ConflictCheckResult.HasConflict(conflictingRoute)
        } else {
            ConflictCheckResult.NoConflict
        }
    }
}