// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/infrastructure/DriverAbsenceEntity.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsence
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceType
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "driver_absences",
    indexes = [
        Index(name = "idx_driver_absences_company", columnList = "company_id"),
        Index(name = "idx_driver_absences_driver", columnList = "company_id, driver_id"),
        Index(name = "idx_driver_absences_dates", columnList = "company_id, driver_id, start_date, end_date"),
        Index(name = "idx_driver_absences_status", columnList = "company_id, status"),
        Index(name = "idx_driver_absences_active", columnList = "company_id, driver_id, status, start_date, end_date"),
        Index(name = "idx_driver_absences_date_range", columnList = "company_id, start_date, end_date")
    ]
)
class DriverAbsenceEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "driver_id", nullable = false, length = 50)
    val driverId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type", nullable = false, length = 30)
    val absenceType: DriverAbsenceType,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(columnDefinition = "text")
    val reason: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: DriverAbsenceStatus,

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
    fun toDomain() = DriverAbsence(
        id = DriverAbsenceId(id),
        companyId = CompanyId(companyId),
        driverId = DriverId(driverId),
        absenceType = absenceType,
        startDate = startDate,
        endDate = endDate,
        reason = reason,
        status = status,
        createdBy = UserId(createdBy),
        createdByRole = createdByRole,
        createdAt = createdAt,
        cancelledAt = cancelledAt,
        cancellationReason = cancellationReason
    )

    companion object {
        fun fromDomain(absence: DriverAbsence) = DriverAbsenceEntity(
            id = absence.id.value,
            companyId = absence.companyId.value,
            driverId = absence.driverId.value,
            absenceType = absence.absenceType,
            startDate = absence.startDate,
            endDate = absence.endDate,
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