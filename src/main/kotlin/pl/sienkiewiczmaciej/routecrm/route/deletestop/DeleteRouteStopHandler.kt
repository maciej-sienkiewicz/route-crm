// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/deletestop/DeleteRouteStopHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.deletestop

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.updatestop.RouteStopNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteRouteStopCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopId: RouteStopId
)

@Component
class DeleteRouteStopHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteRouteStopCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        require(route.canDeleteStops()) {
            "Cannot delete stops from route with status ${route.status}"
        }

        val stop = stopRepository.findById(command.companyId, command.stopId)
            ?: throw RouteStopNotFoundException(command.stopId)

        require(stop.routeId == command.routeId) {
            "Stop ${command.stopId.value} does not belong to route ${command.routeId.value}"
        }

        require(stop.canBeDeleted()) {
            "Cannot delete stop: already executed"
        }

        stopRepository.delete(command.companyId, command.stopId)

        val remainingStops = stopRepository.findByRoute(command.companyId, command.routeId)
        val reorderedStops = remainingStops
            .sortedBy { it.stopOrder }
            .mapIndexed { index, stop -> stop.updateOrder(index + 1) }

        stopRepository.saveAll(reorderedStops)
    }
}