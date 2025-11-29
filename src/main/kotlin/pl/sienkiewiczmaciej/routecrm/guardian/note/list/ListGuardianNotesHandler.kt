package pl.sienkiewiczmaciej.routecrm.guardian.note.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteCategory
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class ListGuardianNotesQuery(
    val companyId: CompanyId,
    val guardianId: GuardianId
)

data class GuardianNoteListItem(
    val id: GuardianNoteId,
    val guardianId: GuardianId,
    val category: GuardianNoteCategory,
    val content: String,
    val createdByName: String,
    val createdAt: Instant
)

@Component
class ListGuardianNotesHandler(
    private val noteRepository: GuardianNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListGuardianNotesQuery): List<GuardianNoteListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val notes = noteRepository.findByGuardian(query.companyId, query.guardianId)

        return notes.map { note ->
            GuardianNoteListItem(
                id = note.id,
                guardianId = note.guardianId,
                category = note.category,
                content = note.content,
                createdByName = note.createdByName,
                createdAt = note.createdAt
            )
        }
    }
}