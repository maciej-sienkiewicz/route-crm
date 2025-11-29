package pl.sienkiewiczmaciej.routecrm.guardian.note.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianNotFoundException
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNote
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteCategory
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class CreateGuardianNoteCommand(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val category: GuardianNoteCategory,
    val content: String
)

data class CreateGuardianNoteResult(
    val id: GuardianNoteId,
    val guardianId: GuardianId,
    val category: GuardianNoteCategory,
    val content: String,
    val createdByName: String,
    val createdAt: Instant
)

@Component
class CreateGuardianNoteHandler(
    private val guardianRepository: GuardianRepository,
    private val noteRepository: GuardianNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateGuardianNoteCommand): CreateGuardianNoteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        guardianRepository.findById(command.companyId, command.guardianId)
            ?: throw GuardianNotFoundException(command.guardianId)

        val note = GuardianNote.create(
            guardianId = command.guardianId,
            companyId = command.companyId,
            category = command.category,
            content = command.content,
            createdBy = principal.userId,
            createdByName = "${principal.firstName} ${principal.lastName}"
        )

        val saved = noteRepository.save(note)

        return CreateGuardianNoteResult(
            id = saved.id,
            guardianId = saved.guardianId,
            category = saved.category,
            content = saved.content,
            createdByName = saved.createdByName,
            createdAt = saved.createdAt
        )
    }
}