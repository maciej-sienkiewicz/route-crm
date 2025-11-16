// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/create/CreateDriverNoteHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNote
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteCategory
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class CreateDriverNoteCommand(
    val companyId: CompanyId,
    val driverId: DriverId,
    val category: DriverNoteCategory,
    val content: String
)

data class CreateDriverNoteResult(
    val id: DriverNoteId,
    val driverId: DriverId,
    val category: DriverNoteCategory,
    val content: String,
    val createdByName: String,
    val createdAt: Instant
)

@Component
class CreateDriverNoteHandler(
    private val noteRepository: DriverNoteRepository,
    private val driverRepository: DriverRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateDriverNoteCommand): CreateDriverNoteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val driver = driverRepository.findById(command.companyId, command.driverId)
            ?: throw DriverNotFoundException(command.driverId)

        val note = DriverNote.create(
            companyId = command.companyId,
            driverId = command.driverId,
            category = command.category,
            content = command.content,
            createdBy = principal.userId,
            createdByName = "${principal.userId.value}"
        )

        val saved = noteRepository.save(note)

        return CreateDriverNoteResult(
            id = saved.id,
            driverId = saved.driverId,
            category = saved.category,
            content = saved.content,
            createdByName = saved.createdByName,
            createdAt = saved.createdAt
        )
    }
}