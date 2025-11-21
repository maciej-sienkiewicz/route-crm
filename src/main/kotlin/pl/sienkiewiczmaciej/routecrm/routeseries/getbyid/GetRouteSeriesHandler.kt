// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/getbyid/GetRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeries
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class GetRouteSeriesQuery(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId
)

@Component
class GetRouteSeriesHandler(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetRouteSeriesQuery): RouteSeries {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        return routeSeriesRepository.findById(query.companyId, query.seriesId)
            ?: throw NotFoundException("Route series ${query.seriesId.value} not found")
    }
}