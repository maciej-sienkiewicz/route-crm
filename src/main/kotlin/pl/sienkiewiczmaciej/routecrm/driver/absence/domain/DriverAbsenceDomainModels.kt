// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/domain/DriverAbsenceDomainModels.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate
import java.util.*

@JvmInline
value class DriverAbsenceId(val value: String) {
    companion object {
        fun generate() = DriverAbsenceId("DABS-${UUID.randomUUID()}")
        fun from(value: String) = DriverAbsenceId(value)
    }
}

enum class DriverAbsenceType {
    SICK_LEAVE,
    VACATION,
    PERSONAL_LEAVE,
    UNPAID_LEAVE,
    OTHER
}

enum class DriverAbsenceStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

data class DriverAbsence(
    val id: DriverAbsenceId,
    val companyId: CompanyId,
    val driverId: DriverId,
    val absenceType: DriverAbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
    val status: DriverAbsenceStatus,
    val createdBy: UserId,
    val createdByRole: UserRole,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val cancellationReason: String?
) {
    companion object {
        fun create(
            companyId: CompanyId,
            driverId: DriverId,
            absenceType: DriverAbsenceType,
            startDate: LocalDate,
            endDate: LocalDate,
            reason: String?,
            createdBy: UserId,
            createdByRole: UserRole
        ): DriverAbsence {
            validateDateRange(startDate, endDate)

            return DriverAbsence(
                id = DriverAbsenceId.generate(),
                companyId = companyId,
                driverId = driverId,
                absenceType = absenceType,
                startDate = startDate,
                endDate = endDate,
                reason = reason?.trim(),
                status = calculateInitialStatus(startDate, endDate),
                createdBy = createdBy,
                createdByRole = createdByRole,
                createdAt = Instant.now(),
                cancelledAt = null,
                cancellationReason = null
            )
        }

        private fun validateDateRange(startDate: LocalDate, endDate: LocalDate) {
            require(!startDate.isBefore(LocalDate.now())) {
                "Absence cannot start in the past"
            }
            require(!endDate.isBefore(startDate)) {
                "End date must be on or after start date"
            }
            require(endDate.isBefore(LocalDate.now().plusYears(1))) {
                "Absence cannot be scheduled more than 1 year in advance"
            }
        }

        private fun calculateInitialStatus(startDate: LocalDate, endDate: LocalDate): DriverAbsenceStatus {
            val today = LocalDate.now()
            return when {
                today.isBefore(startDate) -> DriverAbsenceStatus.PLANNED
                today.isAfter(endDate) -> DriverAbsenceStatus.COMPLETED
                else -> DriverAbsenceStatus.ACTIVE
            }
        }
    }

    fun cancel(reason: String): DriverAbsence {
        require(status != DriverAbsenceStatus.CANCELLED) { "Absence is already cancelled" }
        require(status != DriverAbsenceStatus.COMPLETED) { "Cannot cancel completed absence" }

        return copy(
            status = DriverAbsenceStatus.CANCELLED,
            cancelledAt = Instant.now(),
            cancellationReason = reason.trim()
        )
    }

    fun getCurrentStatus(): DriverAbsenceStatus {
        if (status == DriverAbsenceStatus.CANCELLED) return DriverAbsenceStatus.CANCELLED

        val today = LocalDate.now()
        return when {
            today.isBefore(startDate) -> DriverAbsenceStatus.PLANNED
            today.isAfter(endDate) -> DriverAbsenceStatus.COMPLETED
            else -> DriverAbsenceStatus.ACTIVE
        }
    }

    fun isActiveOn(date: LocalDate): Boolean {
        if (status == DriverAbsenceStatus.CANCELLED) return false
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    fun conflictsWithRoute(routeDate: LocalDate): Boolean {
        return isActiveOn(routeDate)
    }
}