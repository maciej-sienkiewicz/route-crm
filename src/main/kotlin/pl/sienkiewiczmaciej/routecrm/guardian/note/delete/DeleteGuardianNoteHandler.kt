package pl.sienkiewiczmaciej.routecrm.guardian.note.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteRepository
import pl.sienkiewiczmaciej.routecrm.guardian.note.update.GuardianNoteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteGuardianNoteCommand(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val noteId: GuardianNoteId
)

@Component
class DeleteGuardianNoteHandler(
    private val noteRepository: GuardianNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteGuardianNoteCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val note = noteRepository.findById(command.companyId, command.noteId)
            ?: throw GuardianNoteNotFoundException(command.noteId)

        require(note.guardianId == command.guardianId) {
            "Note ${command.noteId.value} does not belong to guardian ${command.guardianId.value}"
        }

        noteRepository.delete(command.companyId, command.noteId)
    }
}