// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/cancel/CancelAbsenceHandler.kt
package pl.sienkiewiczmaciej.routecrm.absence.cancel

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.absence.getbyid.AbsenceNotFoundException
import pl.sienkiewiczmaciej.routecrm.absence.infrastructure.services.AbsenceApplicationService
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class CancelAbsenceCommand(
    val companyId: CompanyId,
    val id: ChildAbsenceId,
    val reason: String
)

data class CancelAbsenceResult(
    val id: ChildAbsenceId,
    val cancelledAt: Instant,
    val affectedRouteStopIds: List<String>
)

@Component
class CancelAbsenceHandler(
    private val absenceRepository: ChildAbsenceRepository,
    private val absenceApplicationService: AbsenceApplicationService,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CancelAbsenceCommand): CancelAbsenceResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val absence = absenceRepository.findById(command.companyId, command.id)
            ?: throw AbsenceNotFoundException(command.id)

        if (principal.role == UserRole.GUARDIAN) {
            require(principal.guardianId != null) { "Guardian must have guardianId" }
            require(absence.createdBy == principal.userId) {
                "Guardian can only cancel their own absences"
            }
        }

        val cancelled = absence.cancel(command.reason)
        absenceRepository.save(cancelled)

        val affectedStopIds = absenceApplicationService.removeAbsenceFromRouteStops(command.id)

        return CancelAbsenceResult(
            id = cancelled.id,
            cancelledAt = cancelled.cancelledAt!!,
            affectedRouteStopIds = affectedStopIds
        )
    }
}