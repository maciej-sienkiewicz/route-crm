// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/delete/DeleteDriverNoteHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteRepository
import pl.sienkiewiczmaciej.routecrm.driver.notes.getbyid.DriverNoteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteDriverNoteCommand(
    val companyId: CompanyId,
    val id: DriverNoteId
)

@Component
class DeleteDriverNoteHandler(
    private val noteRepository: DriverNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteDriverNoteCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val note = noteRepository.findById(command.companyId, command.id)
            ?: throw DriverNoteNotFoundException(command.id)

        noteRepository.delete(command.companyId, command.id)
    }
}