// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/create/CreateAbsenceHandler.kt
package pl.sienkiewiczmaciej.routecrm.absence.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.absence.domain.*
import pl.sienkiewiczmaciej.routecrm.absence.infrastructure.services.AbsenceApplicationService
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.ScheduleNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class CreateAbsenceCommand(
    val companyId: CompanyId,
    val childId: ChildId,
    val absenceType: AbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val scheduleId: ScheduleId?,
    val reason: String?
)

data class CreateAbsenceResult(
    val id: ChildAbsenceId,
    val childId: ChildId,
    val absenceType: AbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val scheduleId: ScheduleId?,
    val reason: String?,
    val status: AbsenceStatus,
    val affectedRouteStops: Int
)

@Component
class CreateAbsenceHandler(
    private val absenceRepository: ChildAbsenceRepository,
    private val childRepository: ChildRepository,
    private val scheduleRepository: ScheduleRepository,
    private val absenceApplicationService: AbsenceApplicationService,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService,
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateAbsenceCommand): CreateAbsenceResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        if (principal.role == UserRole.GUARDIAN) {
            require(principal.guardianId != null) { "Guardian must have guardianId" }
            // TODO: Verify guardian owns this child
        }

        val child = childRepository.findById(command.companyId, command.childId)
            ?: throw ChildNotFoundException(command.childId)

        require(child.status == ChildStatus.ACTIVE) {
            "Cannot create absence for inactive child"
        }

        if (command.absenceType == AbsenceType.SPECIFIC_SCHEDULE) {
            require(command.scheduleId != null) { "Schedule ID required for SPECIFIC_SCHEDULE absence type" }

            val schedule = scheduleRepository.findById(command.companyId, command.scheduleId)
                ?: throw ScheduleNotFoundException(command.scheduleId)

            require(schedule.childId == command.childId) {
                "Schedule ${command.scheduleId.value} does not belong to child ${command.childId.value}"
            }
        }

        val absence = when (command.absenceType) {
            AbsenceType.FULL_DAY -> ChildAbsence.createFullDay(
                companyId = command.companyId,
                childId = command.childId,
                startDate = command.startDate,
                endDate = command.endDate,
                reason = command.reason,
                createdBy = principal.userId,
                createdByRole = principal.role
            )
            AbsenceType.SPECIFIC_SCHEDULE -> ChildAbsence.createSpecificSchedule(
                companyId = command.companyId,
                childId = command.childId,
                scheduleId = command.scheduleId!!,
                startDate = command.startDate,
                endDate = command.endDate,
                reason = command.reason,
                createdBy = principal.userId,
                createdByRole = principal.role
            )
        }

        val saved = absenceRepository.save(absence)

        val affectedStops = absenceApplicationService.applyAbsenceToRouteStops(saved)
        val affectedRoutes = affectedStops.map { it.routeId }


        return CreateAbsenceResult(
            id = saved.id,
            childId = saved.childId,
            absenceType = saved.absenceType,
            startDate = saved.startDate,
            endDate = saved.endDate,
            scheduleId = saved.scheduleId,
            reason = saved.reason,
            status = saved.status,
            affectedRouteStops = affectedStops.size
        )
    }
}