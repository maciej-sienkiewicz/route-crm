// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/cancelstop/CancelRouteStopHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.cancelstop

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.updatestop.RouteStopNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class CancelRouteStopCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopId: RouteStopId,
    val reason: String
)

data class CancelRouteStopResult(
    val id: RouteStopId,
    val isCancelled: Boolean,
    val cancelledAt: Instant?,
    val cancellationReason: String?
)

@Component
class CancelRouteStopHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CancelRouteStopCommand): CancelRouteStopResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        val stop = stopRepository.findById(command.companyId, command.stopId)
            ?: throw RouteStopNotFoundException(command.stopId)

        require(stop.routeId == command.routeId) {
            "Stop ${command.stopId.value} does not belong to route ${command.routeId.value}"
        }

        val cancelled = stop.cancel(command.reason)
        val saved = stopRepository.save(cancelled)

        return CancelRouteStopResult(
            id = saved.id,
            isCancelled = saved.isCancelled,
            cancelledAt = saved.cancelledAt,
            cancellationReason = saved.cancellationReason
        )
    }
}