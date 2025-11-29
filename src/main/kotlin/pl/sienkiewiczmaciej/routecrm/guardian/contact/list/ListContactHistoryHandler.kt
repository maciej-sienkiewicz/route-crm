package pl.sienkiewiczmaciej.routecrm.guardian.contact.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryId
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryRepository
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactType
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class ListContactHistoryQuery(
    val companyId: CompanyId,
    val guardianId: GuardianId
)

data class ContactHistoryListItem(
    val id: ContactHistoryId,
    val type: ContactType,
    val subject: String,
    val notes: String,
    val contactedAt: Instant,
    val handledByName: String
)

@Component
class ListContactHistoryHandler(
    private val contactRepository: ContactHistoryRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListContactHistoryQuery): List<ContactHistoryListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val contacts = contactRepository.findByGuardian(query.companyId, query.guardianId)

        return contacts.map { contact ->
            ContactHistoryListItem(
                id = contact.id,
                type = contact.type,
                subject = contact.subject,
                notes = contact.notes,
                contactedAt = contact.contactedAt,
                handledByName = contact.handledByName
            )
        }
    }
}