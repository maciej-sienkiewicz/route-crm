package pl.sienkiewiczmaciej.routecrm.guardian.contact.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistory
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryId
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryRepository
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactType
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class CreateContactHistoryCommand(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val type: ContactType,
    val subject: String,
    val notes: String
)

data class CreateContactHistoryResult(
    val id: ContactHistoryId,
    val type: ContactType,
    val subject: String,
    val notes: String,
    val contactedAt: Instant,
    val handledByName: String
)

@Component
class CreateContactHistoryHandler(
    private val guardianRepository: GuardianRepository,
    private val contactRepository: ContactHistoryRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateContactHistoryCommand): CreateContactHistoryResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        guardianRepository.findById(command.companyId, command.guardianId)
            ?: throw GuardianNotFoundException(command.guardianId)

        val contact = ContactHistory.create(
            guardianId = command.guardianId,
            companyId = command.companyId,
            type = command.type,
            subject = command.subject,
            notes = command.notes,
            handledBy = principal.userId,
            handledByName = "${principal.firstName} ${principal.lastName}"
        )

        val saved = contactRepository.save(contact)

        return CreateContactHistoryResult(
            id = saved.id,
            type = saved.type,
            subject = saved.subject,
            notes = saved.notes,
            contactedAt = saved.contactedAt,
            handledByName = saved.handledByName
        )
    }
}