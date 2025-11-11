// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/domain/AbsenceDomainModels.kt
package pl.sienkiewiczmaciej.routecrm.absence.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate
import java.util.*

@JvmInline
value class ChildAbsenceId(val value: String) {
    companion object {
        fun generate() = ChildAbsenceId("ABS-${UUID.randomUUID()}")
        fun from(value: String) = ChildAbsenceId(value)
    }
}

enum class AbsenceType {
    FULL_DAY,
    SPECIFIC_SCHEDULE
}

enum class AbsenceStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

data class ChildAbsence(
    val id: ChildAbsenceId,
    val companyId: CompanyId,
    val childId: ChildId,
    val absenceType: AbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val scheduleId: ScheduleId?,
    val reason: String?,
    val status: AbsenceStatus,
    val createdBy: UserId,
    val createdByRole: UserRole,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val cancellationReason: String?
) {
    companion object {
        fun createFullDay(
            companyId: CompanyId,
            childId: ChildId,
            startDate: LocalDate,
            endDate: LocalDate,
            reason: String?,
            createdBy: UserId,
            createdByRole: UserRole
        ): ChildAbsence {
            validateDateRange(startDate, endDate)

            return ChildAbsence(
                id = ChildAbsenceId.generate(),
                companyId = companyId,
                childId = childId,
                absenceType = AbsenceType.FULL_DAY,
                startDate = startDate,
                endDate = endDate,
                scheduleId = null,
                reason = reason?.trim(),
                status = calculateInitialStatus(startDate, endDate),
                createdBy = createdBy,
                createdByRole = createdByRole,
                createdAt = Instant.now(),
                cancelledAt = null,
                cancellationReason = null
            )
        }

        fun createSpecificSchedule(
            companyId: CompanyId,
            childId: ChildId,
            scheduleId: ScheduleId,
            startDate: LocalDate,
            endDate: LocalDate,
            reason: String?,
            createdBy: UserId,
            createdByRole: UserRole
        ): ChildAbsence {
            validateDateRange(startDate, endDate)

            return ChildAbsence(
                id = ChildAbsenceId.generate(),
                companyId = companyId,
                childId = childId,
                absenceType = AbsenceType.SPECIFIC_SCHEDULE,
                startDate = startDate,
                endDate = endDate,
                scheduleId = scheduleId,
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

        private fun calculateInitialStatus(startDate: LocalDate, endDate: LocalDate): AbsenceStatus {
            val today = LocalDate.now()
            return when {
                today.isBefore(startDate) -> AbsenceStatus.PLANNED
                today.isAfter(endDate) -> AbsenceStatus.COMPLETED
                else -> AbsenceStatus.ACTIVE
            }
        }
    }

    fun cancel(reason: String): ChildAbsence {
        require(status != AbsenceStatus.CANCELLED) { "Absence is already cancelled" }
        require(status != AbsenceStatus.COMPLETED) { "Cannot cancel completed absence" }

        return copy(
            status = AbsenceStatus.CANCELLED,
            cancelledAt = Instant.now(),
            cancellationReason = reason.trim()
        )
    }

    fun getCurrentStatus(): AbsenceStatus {
        if (status == AbsenceStatus.CANCELLED) return AbsenceStatus.CANCELLED

        val today = LocalDate.now()
        return when {
            today.isBefore(startDate) -> AbsenceStatus.PLANNED
            today.isAfter(endDate) -> AbsenceStatus.COMPLETED
            else -> AbsenceStatus.ACTIVE
        }
    }

    fun isActiveOn(date: LocalDate): Boolean {
        if (status == AbsenceStatus.CANCELLED) return false
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    fun coversSchedule(scheduleId: ScheduleId, date: LocalDate): Boolean {
        if (!isActiveOn(date)) return false

        return when (absenceType) {
            AbsenceType.FULL_DAY -> true
            AbsenceType.SPECIFIC_SCHEDULE -> this.scheduleId == scheduleId
        }
    }

    fun affectsRoute(date: LocalDate): Boolean {
        return isActiveOn(date)
    }
}

data class AbsenceConflict(
    val absence: ChildAbsence,
    val conflictType: ConflictType
) {
    enum class ConflictType {
        FULL_DAY_ABSENCE,
        SCHEDULE_SPECIFIC_ABSENCE
    }
}