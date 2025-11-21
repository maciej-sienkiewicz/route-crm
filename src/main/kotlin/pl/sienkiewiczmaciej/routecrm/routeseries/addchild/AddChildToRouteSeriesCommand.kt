// routeseries/addchild/AddChildToRouteSeriesCommand.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class AddChildToRouteSeriesCommand(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val pickupStopOrder: Int,
    val dropoffStopOrder: Int,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null
)

data class AddChildToSeriesResult(
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val existingRoutesUpdated: Int,
    val conflictResolved: Boolean = false
)

class RouteSeriesNotFoundException(id: RouteSeriesId) :
    RuntimeException("Route series ${id.value} not found")