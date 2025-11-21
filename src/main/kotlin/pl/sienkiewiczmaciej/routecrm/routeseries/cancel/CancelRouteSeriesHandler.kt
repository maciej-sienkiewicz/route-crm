// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/cancel/CancelRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.cancel

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

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

@Component
class CancelRouteSeriesHandler(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val routeRepository: RouteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: CancelRouteSeriesCommand
    ): CancelRouteSeriesResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val series = routeSeriesRepository.findById(command.companyId, command.seriesId)
            ?: throw pl.sienkiewiczmaciej.routecrm.routeseries.addchild.RouteSeriesNotFoundException(command.seriesId)

        val cancelledSeries = series.cancel(principal.userId, command.reason)
        routeSeriesRepository.save(cancelledSeries)

        var cancelledRoutesCount = 0

        if (command.cancelFutureRoutes) {
            val futureRoutes = routeRepository.findBySeries(
                companyId = command.companyId,
                seriesId = command.seriesId,
                fromDate = LocalDate.now(),
                statuses = setOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS)
            )

            futureRoutes.forEach { route ->
                val cancelled = route.cancel()
                routeRepository.save(cancelled)
                cancelledRoutesCount++
            }
        }

        return CancelRouteSeriesResult(
            seriesId = cancelledSeries.id,
            status = cancelledSeries.status,
            futureRoutesCancelled = cancelledRoutesCount
        )
    }
}