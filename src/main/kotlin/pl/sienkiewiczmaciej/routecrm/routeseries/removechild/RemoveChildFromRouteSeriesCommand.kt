// routeseries/removechild/RemoveChildFromRouteSeriesCommand.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.removechild

import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class RemoveChildFromRouteSeriesCommand(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val effectiveFrom: LocalDate,
    val cancelExistingStops: Boolean = true
)

data class RemoveChildFromSeriesResult(
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate,
    val stopsCancelled: Int
)