// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/getbyid/GetAbsenceHandler.kt
package pl.sienkiewiczmaciej.routecrm.absence.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class GetAbsenceQuery(
    val companyId: CompanyId,
    val id: ChildAbsenceId
)

data class AbsenceDetail(
    val id: ChildAbsenceId,
    val childId: ChildId,
    val absenceType: AbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val scheduleId: ScheduleId?,
    val scheduleName: String?,
    val reason: String?,
    val status: AbsenceStatus,
    val createdByRole: UserRole,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val cancellationReason: String?
)

class AbsenceNotFoundException(id: ChildAbsenceId) : NotFoundException("Absence ${id.value} not found")

@Component
class GetAbsenceHandler(
    private val absenceRepository: ChildAbsenceRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetAbsenceQuery): AbsenceDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val absence = absenceRepository.findById(query.companyId, query.id)
            ?: throw AbsenceNotFoundException(query.id)

        if (principal.role == UserRole.GUARDIAN) {
            require(principal.guardianId != null) { "Guardian must have guardianId" }
            // TODO: Verify guardian owns this child
        }

        return AbsenceDetail(
            id = absence.id,
            childId = absence.childId,
            absenceType = absence.absenceType,
            startDate = absence.startDate,
            endDate = absence.endDate,
            scheduleId = absence.scheduleId,
            scheduleName = null,
            reason = absence.reason,
            status = absence.getCurrentStatus(),
            createdByRole = absence.createdByRole,
            createdAt = absence.createdAt,
            cancelledAt = absence.cancelledAt,
            cancellationReason = absence.cancellationReason
        )
    }
}