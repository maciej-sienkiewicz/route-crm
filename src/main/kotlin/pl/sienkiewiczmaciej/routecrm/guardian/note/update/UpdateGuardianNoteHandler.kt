package pl.sienkiewiczmaciej.routecrm.guardian.note.update

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteCategory
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class UpdateGuardianNoteCommand(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val noteId: GuardianNoteId,
    val category: GuardianNoteCategory,
    val content: String
)

data class UpdateGuardianNoteResult(
    val id: GuardianNoteId,
    val category: GuardianNoteCategory,
    val content: String,
    val createdByName: String,
    val createdAt: Instant
)

class GuardianNoteNotFoundException(id: GuardianNoteId) : NotFoundException("Guardian note ${id.value} not found")

@Component
class UpdateGuardianNoteHandler(
    private val noteRepository: GuardianNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateGuardianNoteCommand): UpdateGuardianNoteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val note = noteRepository.findById(command.companyId, command.noteId)
            ?: throw GuardianNoteNotFoundException(command.noteId)

        require(note.guardianId == command.guardianId) {
            "Note ${command.noteId.value} does not belong to guardian ${command.guardianId.value}"
        }

        val updated = note.update(
            category = command.category,
            content = command.content
        )

        val saved = noteRepository.save(updated)

        return UpdateGuardianNoteResult(
            id = saved.id,
            category = saved.category,
            content = saved.content,
            createdByName = saved.createdByName,
            createdAt = saved.createdAt
        )
    }
}