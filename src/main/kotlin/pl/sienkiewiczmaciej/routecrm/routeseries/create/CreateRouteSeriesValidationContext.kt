// routeseries/create/CreateRouteSeriesValidationContext.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.create

import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Vehicle

data class CreateRouteSeriesValidationContext(
    val sourceRoute: Route,
    val sourceRouteStops: List<RouteStop>,
    val driver: Driver,
    val vehicle: Vehicle,
    val children: Map<ChildId, Child>,
    val schedules: Map<ScheduleId, Schedule>
)