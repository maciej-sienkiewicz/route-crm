// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/create/ValidationContext.kt
package pl.sienkiewiczmaciej.routecrm.route.create

import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Vehicle

/**
 * Context containing pre-loaded aggregates needed for CreateRoute validation.
 * All data is loaded once by ValidationContextBuilder to avoid N+1 queries.
 */
data class CreateRouteValidationContext(
    val driver: Driver,
    val vehicle: Vehicle,
    val children: Map<ChildId, Child>,
    val schedules: Map<ScheduleId, Schedule>,
    val existingDriverRoutes: List<Route>,
    val existingVehicleRoutes: List<Route>
)