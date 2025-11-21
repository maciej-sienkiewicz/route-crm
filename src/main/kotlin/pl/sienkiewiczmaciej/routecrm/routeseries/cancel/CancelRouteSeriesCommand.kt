// routeseries/cancel/CancelRouteSeriesCommand.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.cancel

import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class CancelRouteSeriesCommand(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val reason: String,
    val cancelFutureRoutes: Boolean = true
)

data class CancelRouteSeriesResult(
    val seriesId: RouteSeriesId,
    val status: RouteSeriesStatus,
    val futureRoutesCancelled: Int
)