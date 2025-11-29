package pl.sienkiewiczmaciej.routecrm.guardian.contact.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryId
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteContactHistoryCommand(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val contactId: ContactHistoryId
)

class ContactHistoryNotFoundException(id: ContactHistoryId) : NotFoundException("Contact history ${id.value} not found")

@Component
class DeleteContactHistoryHandler(
    private val contactRepository: ContactHistoryRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteContactHistoryCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val contact = contactRepository.findById(command.companyId, command.contactId)
            ?: throw ContactHistoryNotFoundException(command.contactId)

        require(contact.guardianId == command.guardianId) {
            "Contact ${command.contactId.value} does not belong to guardian ${command.guardianId.value}"
        }

        contactRepository.delete(command.companyId, command.contactId)
    }
}