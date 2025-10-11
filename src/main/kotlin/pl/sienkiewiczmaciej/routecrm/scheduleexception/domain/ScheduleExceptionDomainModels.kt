package pl.sienkiewiczmaciej.routecrm.scheduleexception.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate
import java.util.*

@JvmInline
value class ScheduleExceptionId(val value: String) {
    companion object {
        fun generate() = ScheduleExceptionId("SE-${UUID.randomUUID()}")
        fun from(value: String) = ScheduleExceptionId(value)
    }
}

data class ScheduleException(
    val id: ScheduleExceptionId,
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val exceptionDate: LocalDate,
    val notes: String?,
    val createdBy: UserId,
    val createdByRole: UserRole,
    val createdAt: Instant
) {
    companion object {
        fun create(
            companyId: CompanyId,
            scheduleId: ScheduleId,
            childId: ChildId,
            exceptionDate: LocalDate,
            notes: String?,
            createdBy: UserId,
            createdByRole: UserRole
        ): ScheduleException {
            require(!exceptionDate.isBefore(LocalDate.now())) {
                "Exception date cannot be in the past"
            }

            return ScheduleException(
                id = ScheduleExceptionId.generate(),
                companyId = companyId,
                scheduleId = scheduleId,
                childId = childId,
                exceptionDate = exceptionDate,
                notes = notes?.trim(),
                createdBy = createdBy,
                createdByRole = createdByRole,
                createdAt = Instant.now()
            )
        }
    }
}