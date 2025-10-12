package pl.sienkiewiczmaciej.routecrm.route.note

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteNote
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteNoteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteNoteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class AddRouteNoteCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val content: String
)

data class AddNoteResult(
    val id: RouteNoteId,
    val routeId: RouteId,
    val authorName: String,
    val content: String,
    val createdAt: Instant
)

@Component
class AddRouteNoteHandler(
    private val routeRepository: RouteRepository,
    private val routeNoteRepository: RouteNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: AddRouteNoteCommand): AddNoteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        if (principal.role == UserRole.DRIVER && principal.driverId != null) {
            require(route.driverId == DriverId.from(principal.driverId)) {
                "Driver can only add notes to their own routes"
            }
        }

        val note = RouteNote.create(
            companyId = command.companyId,
            routeId = command.routeId,
            authorUserId = principal.userId.value,
            authorName = "${principal.firstName} ${principal.lastName}",
            content = command.content
        )

        val saved = routeNoteRepository.save(note)

        return AddNoteResult(
            id = saved.id,
            routeId = saved.routeId,
            authorName = saved.authorName,
            content = saved.content,
            createdAt = saved.createdAt
        )
    }
}