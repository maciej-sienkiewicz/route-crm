package pl.sienkiewiczmaciej.routecrm.child.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteChildCommand(
    val companyId: CompanyId,
    val id: ChildId
)

@Component
class DeleteChildHandler(
    private val childRepository: ChildRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteChildCommand) {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val child = childRepository.findById(command.companyId, command.id)
            ?: throw ChildNotFoundException(command.id)

        childRepository.delete(command.companyId, command.id)
    }
}