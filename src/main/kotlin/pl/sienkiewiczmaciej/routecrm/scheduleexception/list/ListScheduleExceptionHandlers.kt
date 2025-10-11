package pl.sienkiewiczmaciej.routecrm.scheduleexception.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class ListScheduleExceptionsQuery(
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val from: LocalDate?,
    val to: LocalDate?
)

data class ListChildExceptionsQuery(
    val companyId: CompanyId,
    val childId: ChildId,
    val from: LocalDate?,
    val to: LocalDate?
)

data class ScheduleExceptionListItem(
    val id: ScheduleExceptionId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val exceptionDate: LocalDate,
    val notes: String?,
    val createdByRole: UserRole,
    val createdAt: Instant
)

@Component
class ListScheduleExceptionsHandler(
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(
        principal: UserPrincipal,
        query: ListScheduleExceptionsQuery
    ): List<ScheduleExceptionListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val exceptions = scheduleExceptionRepository.findBySchedule(
            companyId = query.companyId,
            scheduleId = query.scheduleId,
            from = query.from,
            to = query.to
        )

        return exceptions.map { exception ->
            ScheduleExceptionListItem(
                id = exception.id,
                scheduleId = exception.scheduleId,
                childId = exception.childId,
                exceptionDate = exception.exceptionDate,
                notes = exception.notes,
                createdByRole = exception.createdByRole,
                createdAt = exception.createdAt
            )
        }
    }
}

@Component
class ListChildExceptionsHandler(
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(
        principal: UserPrincipal,
        query: ListChildExceptionsQuery
    ): List<ScheduleExceptionListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val exceptions = scheduleExceptionRepository.findByChild(
            companyId = query.companyId,
            childId = query.childId,
            from = query.from,
            to = query.to
        )

        return exceptions.map { exception ->
            ScheduleExceptionListItem(
                id = exception.id,
                scheduleId = exception.scheduleId,
                childId = exception.childId,
                exceptionDate = exception.exceptionDate,
                notes = exception.notes,
                createdByRole = exception.createdByRole,
                createdAt = exception.createdAt
            )
        }
    }
}