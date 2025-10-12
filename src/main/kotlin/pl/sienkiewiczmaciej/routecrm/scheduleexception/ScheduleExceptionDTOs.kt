package pl.sienkiewiczmaciej.routecrm.scheduleexception

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.create.CreateScheduleExceptionCommand
import pl.sienkiewiczmaciej.routecrm.scheduleexception.create.CreateScheduleExceptionResult
import pl.sienkiewiczmaciej.routecrm.scheduleexception.list.ScheduleExceptionListItem
import pl.sienkiewiczmaciej.routecrm.scheduleexception.stats.ChildExceptionStats
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate

data class CreateScheduleExceptionRequest(
    @field:NotNull(message = "Exception date is required")
    val exceptionDate: LocalDate,

    @field:Size(max = 1000)
    val notes: String?
) {
    fun toCommand(companyId: CompanyId, scheduleId: ScheduleId) = CreateScheduleExceptionCommand(
        companyId = companyId,
        scheduleId = scheduleId,
        exceptionDate = exceptionDate,
        notes = notes
    )
}

data class ScheduleExceptionResponse(
    val id: String,
    val scheduleId: String,
    val childId: String,
    val exceptionDate: LocalDate,
    val notes: String?,
    val createdByRole: UserRole,
    val createdAt: Instant
) {
    companion object {
        fun from(result: CreateScheduleExceptionResult, principal: UserPrincipal) = ScheduleExceptionResponse(
            id = result.id.value,
            scheduleId = result.scheduleId.value,
            childId = result.childId.value,
            exceptionDate = result.exceptionDate,
            notes = result.notes,
            createdByRole = principal.role,
            createdAt = result.createdAt
        )

        fun from(item: ScheduleExceptionListItem) = ScheduleExceptionResponse(
            id = item.id.value,
            scheduleId = item.scheduleId.value,
            childId = item.childId.value,
            exceptionDate = item.exceptionDate,
            notes = item.notes,
            createdByRole = item.createdByRole,
            createdAt = item.createdAt
        )
    }
}

data class ScheduleExceptionsListResponse(
    val exceptions: List<ScheduleExceptionResponse>
) {
    companion object {
        fun from(exceptions: List<ScheduleExceptionListItem>) = ScheduleExceptionsListResponse(
            exceptions = exceptions.map { ScheduleExceptionResponse.from(it) }
        )
    }
}

data class ChildExceptionStatsResponse(
    val childId: String,
    val periodFrom: LocalDate,
    val periodTo: LocalDate,
    val totalExceptions: Int
) {
    companion object {
        fun from(stats: ChildExceptionStats) = ChildExceptionStatsResponse(
            childId = stats.childId.value,
            periodFrom = stats.periodFrom,
            periodTo = stats.periodTo,
            totalExceptions = stats.totalExceptions
        )
    }
}