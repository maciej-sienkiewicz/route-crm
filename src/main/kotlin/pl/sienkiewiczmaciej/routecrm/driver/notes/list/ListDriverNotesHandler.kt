// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/list/ListDriverNotesHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteCategory
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class ListDriverNotesQuery(
    val companyId: CompanyId,
    val driverId: DriverId
)

data class DriverNoteListItem(
    val id: DriverNoteId,
    val category: DriverNoteCategory,
    val content: String,
    val createdByName: String,
    val createdAt: Instant
)

@Component
class ListDriverNotesHandler(
    private val noteRepository: DriverNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListDriverNotesQuery): List<DriverNoteListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val notes = noteRepository.findByDriver(query.companyId, query.driverId)

        return notes.map { note ->
            DriverNoteListItem(
                id = note.id,
                category = note.category,
                content = note.content,
                createdByName = note.createdByName,
                createdAt = note.createdAt
            )
        }
    }
}