// routeseries/getbyid/GetRouteSeriesQuery.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.getbyid

import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class GetRouteSeriesQuery(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId
)