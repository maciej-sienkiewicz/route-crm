package pl.sienkiewiczmaciej.routecrm.scheduleexception.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleException
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "schedule_exceptions",
    indexes = [
        Index(name = "idx_exceptions_company", columnList = "company_id"),
        Index(name = "idx_exceptions_schedule", columnList = "company_id, schedule_id"),
        Index(name = "idx_exceptions_child", columnList = "company_id, child_id"),
        Index(name = "idx_exceptions_date", columnList = "company_id, exception_date"),
        Index(name = "idx_exceptions_child_date", columnList = "company_id, child_id, exception_date")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_exception_schedule_date",
            columnNames = ["schedule_id", "exception_date"]
        )
    ]
)
class ScheduleExceptionEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "schedule_id", nullable = false, length = 50)
    val scheduleId: String,

    @Column(name = "child_id", nullable = false, length = 50)
    val childId: String,

    @Column(name = "exception_date", nullable = false)
    val exceptionDate: LocalDate,

    @Column(columnDefinition = "text")
    val notes: String?,

    @Column(name = "created_by", nullable = false, length = 50)
    val createdBy: String,

    @Column(name = "created_by_role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val createdByRole: UserRole,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = ScheduleException(
        id = ScheduleExceptionId(id),
        companyId = CompanyId(companyId),
        scheduleId = ScheduleId(scheduleId),
        childId = ChildId(childId),
        exceptionDate = exceptionDate,
        notes = notes,
        createdBy = UserId(createdBy),
        createdByRole = createdByRole,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(exception: ScheduleException) = ScheduleExceptionEntity(
            id = exception.id.value,
            companyId = exception.companyId.value,
            scheduleId = exception.scheduleId.value,
            childId = exception.childId.value,
            exceptionDate = exception.exceptionDate,
            notes = exception.notes,
            createdBy = exception.createdBy.value,
            createdByRole = exception.createdByRole,
            createdAt = exception.createdAt
        )
    }
}