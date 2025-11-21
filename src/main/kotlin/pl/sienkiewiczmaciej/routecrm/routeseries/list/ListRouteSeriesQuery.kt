// routeseries/list/ListRouteSeriesQuery.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.list

import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class ListRouteSeriesQuery(
    val companyId: CompanyId,
    val status: RouteSeriesStatus?,
    val pageable: Pageable
)