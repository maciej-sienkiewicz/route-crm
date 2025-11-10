// route/delete/DeleteRouteHandler.kt (UPDATED WITH EVENTS)
package pl.sienkiewiczmaciej.routecrm.route.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteNoteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteRouteCommand(
    val companyId: CompanyId,
    val id: RouteId
)

/**
 * Handler for deleting routes.
 * Simple handler - no events needed for delete operations.
 */
@Component
class DeleteRouteHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val routeNoteRepository: RouteNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteRouteCommand) {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Load and validate route exists
        val route = routeRepository.findById(command.companyId, command.id)
            ?: throw RouteNotFoundException(command.id)

        // 3. Delete cascade: stops, notes, then route
        stopRepository.deleteByRoute(command.companyId, command.id)
        routeNoteRepository.deleteByRoute(command.companyId, command.id)
        routeRepository.delete(command.companyId, command.id)
    }
}