package pl.sienkiewiczmaciej.routecrm.scheduleexception.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.ScheduleNotFoundException
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleException
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class CreateScheduleExceptionCommand(
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val exceptionDate: LocalDate,
    val notes: String?
)

data class CreateScheduleExceptionResult(
    val id: ScheduleExceptionId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val exceptionDate: LocalDate,
    val notes: String?,
    val createdAt: Instant
)

@Component
class CreateScheduleExceptionHandler(
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: CreateScheduleExceptionCommand
    ): CreateScheduleExceptionResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val schedule = scheduleRepository.findById(command.companyId, command.scheduleId)
            ?: throw ScheduleNotFoundException(command.scheduleId)

        if (scheduleExceptionRepository.existsByScheduleAndDate(
                command.companyId,
                command.scheduleId,
                command.exceptionDate
            )
        ) {
            throw IllegalArgumentException(
                "Exception already exists for schedule ${command.scheduleId.value} on ${command.exceptionDate}"
            )
        }

        val exception = ScheduleException.create(
            companyId = command.companyId,
            scheduleId = command.scheduleId,
            childId = schedule.childId,
            exceptionDate = command.exceptionDate,
            notes = command.notes,
            createdBy = principal.userId,
            createdByRole = principal.role
        )

        val saved = scheduleExceptionRepository.save(exception)

        return CreateScheduleExceptionResult(
            id = saved.id,
            scheduleId = saved.scheduleId,
            childId = saved.childId,
            exceptionDate = saved.exceptionDate,
            notes = saved.notes,
            createdAt = saved.createdAt
        )
    }
}