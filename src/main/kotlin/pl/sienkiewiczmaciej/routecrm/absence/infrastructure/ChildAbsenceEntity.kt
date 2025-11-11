// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/infrastructure/ChildAbsenceEntity.kt
package pl.sienkiewiczmaciej.routecrm.absence.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsence
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "child_absences",
    indexes = [
        Index(name = "idx_absences_company", columnList = "company_id"),
        Index(name = "idx_absences_child", columnList = "company_id, child_id"),
        Index(name = "idx_absences_child_dates", columnList = "company_id, child_id, start_date, end_date"),
        Index(name = "idx_absences_schedule", columnList = "company_id, schedule_id"),
        Index(name = "idx_absences_status", columnList = "company_id, status"),
        Index(name = "idx_absences_active", columnList = "company_id, child_id, status, start_date, end_date"),
        Index(name = "idx_absences_date_range", columnList = "company_id, start_date, end_date")
    ]
)
class ChildAbsenceEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "child_id", nullable = false, length = 50)
    val childId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type", nullable = false, length = 20)
    val absenceType: AbsenceType,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(name = "schedule_id", length = 50)
    val scheduleId: String?,

    @Column(columnDefinition = "text")
    val reason: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: AbsenceStatus,

    @Column(name = "created_by", nullable = false, length = 50)
    val createdBy: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_role", nullable = false, length = 20)
    val createdByRole: UserRole,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @Column(name = "cancelled_at")
    val cancelledAt: Instant?,

    @Column(name = "cancellation_reason", columnDefinition = "text")
    val cancellationReason: String?
) {
    fun toDomain() = ChildAbsence(
        id = ChildAbsenceId(id),
        companyId = CompanyId(companyId),
        childId = ChildId(childId),
        absenceType = absenceType,
        startDate = startDate,
        endDate = endDate,
        scheduleId = scheduleId?.let { ScheduleId(it) },
        reason = reason,
        status = status,
        createdBy = UserId(createdBy),
        createdByRole = createdByRole,
        createdAt = createdAt,
        cancelledAt = cancelledAt,
        cancellationReason = cancellationReason
    )

    companion object {
        fun fromDomain(absence: ChildAbsence) = ChildAbsenceEntity(
            id = absence.id.value,
            companyId = absence.companyId.value,
            childId = absence.childId.value,
            absenceType = absence.absenceType,
            startDate = absence.startDate,
            endDate = absence.endDate,
            scheduleId = absence.scheduleId?.value,
            reason = absence.reason,
            status = absence.status,
            createdBy = absence.createdBy.value,
            createdByRole = absence.createdByRole,
            createdAt = absence.createdAt,
            cancelledAt = absence.cancelledAt,
            cancellationReason = absence.cancellationReason
        )
    }
}