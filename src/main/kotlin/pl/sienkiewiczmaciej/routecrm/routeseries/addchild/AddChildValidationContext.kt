// routeseries/addchild/AddChildValidationContext.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeries
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesSchedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule

data class AddChildValidationContext(
    val series: RouteSeries,
    val schedule: Schedule,
    val child: Child,
    val existingSeriesSchedule: RouteSeriesSchedule?,
    val affectedRoutes: List<Route>
)