// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/update/UpdateDriverNoteHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.update

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteCategory
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteRepository
import pl.sienkiewiczmaciej.routecrm.driver.notes.getbyid.DriverNoteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class UpdateDriverNoteCommand(
    val companyId: CompanyId,
    val id: DriverNoteId,
    val category: DriverNoteCategory,
    val content: String
)

data class UpdateDriverNoteResult(
    val id: DriverNoteId,
    val category: DriverNoteCategory,
    val content: String
)

@Component
class UpdateDriverNoteHandler(
    private val noteRepository: DriverNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateDriverNoteCommand): UpdateDriverNoteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val note = noteRepository.findById(command.companyId, command.id)
            ?: throw DriverNoteNotFoundException(command.id)

        val updated = note.update(command.category, command.content)
        noteRepository.save(updated)

        return UpdateDriverNoteResult(
            id = updated.id,
            category = updated.category,
            content = updated.content
        )
    }
}