// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/list/ListAbsencesHandler.kt
package pl.sienkiewiczmaciej.routecrm.absence.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class ListAbsencesByChildQuery(
    val companyId: CompanyId,
    val childId: ChildId,
    val from: LocalDate?,
    val to: LocalDate?,
    val statuses: Set<AbsenceStatus>?
)

data class ListAbsencesByScheduleQuery(
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val from: LocalDate?,
    val to: LocalDate?
)

data class AbsenceListItem(
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

@Component
class ListAbsencesByChildHandler(
    private val absenceRepository: ChildAbsenceRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListAbsencesByChildQuery): List<AbsenceListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        if (principal.role == UserRole.GUARDIAN) {
            require(principal.guardianId != null) { "Guardian must have guardianId" }
            // TODO: Verify guardian owns this child
        }

        val absences = absenceRepository.findByChild(
            companyId = query.companyId,
            childId = query.childId,
            from = query.from,
            to = query.to,
            statuses = query.statuses
        )

        return absences.map { absence ->
            AbsenceListItem(
                id = absence.id,
                childId = absence.childId,
                absenceType = absence.absenceType,
                startDate = absence.startDate,
                endDate = absence.endDate,
                scheduleId = absence.scheduleId,
                scheduleName = null, // TODO: Fetch schedule name if needed
                reason = absence.reason,
                status = absence.getCurrentStatus(),
                createdByRole = absence.createdByRole,
                createdAt = absence.createdAt,
                cancelledAt = absence.cancelledAt,
                cancellationReason = absence.cancellationReason
            )
        }
    }
}

@Component
class ListAbsencesByScheduleHandler(
    private val absenceRepository: ChildAbsenceRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListAbsencesByScheduleQuery): List<AbsenceListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val absences = absenceRepository.findBySchedule(
            companyId = query.companyId,
            scheduleId = query.scheduleId,
            from = query.from,
            to = query.to
        )

        return absences.map { absence ->
            AbsenceListItem(
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
}