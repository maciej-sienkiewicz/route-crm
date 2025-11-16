// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/getbyid/GetDriverNoteHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteCategory
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class GetDriverNoteQuery(
    val companyId: CompanyId,
    val id: DriverNoteId
)

data class DriverNoteDetail(
    val id: DriverNoteId,
    val category: DriverNoteCategory,
    val content: String,
    val createdByName: String,
    val createdAt: Instant
)

class DriverNoteNotFoundException(id: DriverNoteId) :
    NotFoundException("Driver note ${id.value} not found")

@Component
class GetDriverNoteHandler(
    private val noteRepository: DriverNoteRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDriverNoteQuery): DriverNoteDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val note = noteRepository.findById(query.companyId, query.id)
            ?: throw DriverNoteNotFoundException(query.id)

        return DriverNoteDetail(
            id = note.id,
            category = note.category,
            content = note.content,
            createdByName = note.createdByName,
            createdAt = note.createdAt
        )
    }
}