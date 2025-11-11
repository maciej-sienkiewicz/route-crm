// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/AbsenceDTOs.kt
package pl.sienkiewiczmaciej.routecrm.absence

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.absence.cancel.CancelAbsenceCommand
import pl.sienkiewiczmaciej.routecrm.absence.cancel.CancelAbsenceResult
import pl.sienkiewiczmaciej.routecrm.absence.create.CreateAbsenceCommand
import pl.sienkiewiczmaciej.routecrm.absence.create.CreateAbsenceResult
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.absence.getbyid.AbsenceDetail
import pl.sienkiewiczmaciej.routecrm.absence.list.AbsenceListItem
import pl.sienkiewiczmaciej.routecrm.absence.stats.AbsenceStats
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate

data class CreateAbsenceRequest(
    @field:NotNull(message = "Absence type is required")
    val type: AbsenceType,

    @field:NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,

    @field:NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate,

    val scheduleId: String?,

    @field:Size(max = 1000)
    val reason: String?
) {
    fun toCommand(companyId: CompanyId, childId: ChildId) = CreateAbsenceCommand(
        companyId = companyId,
        childId = childId,
        absenceType = type,
        startDate = startDate,
        endDate = endDate,
        scheduleId = scheduleId?.let { ScheduleId.from(it) },
        reason = reason
    )
}

data class AbsenceResponse(
    val id: String,
    val childId: String,
    val type: AbsenceType,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate,
    val scheduleId: String?,
    val scheduleName: String?,
    val reason: String?,
    val status: AbsenceStatus,
    val createdByRole: UserRole,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val cancellationReason: String?,
    val affectedRouteStops: Int?
) {
    companion object {
        fun from(result: CreateAbsenceResult) = AbsenceResponse(
            id = result.id.value,
            childId = result.childId.value,
            type = result.absenceType,
            startDate = result.startDate,
            endDate = result.endDate,
            scheduleId = result.scheduleId?.value,
            scheduleName = null,
            reason = result.reason,
            status = result.status,
            createdByRole = UserRole.ADMIN,
            createdAt = Instant.now(),
            cancelledAt = null,
            cancellationReason = null,
            affectedRouteStops = result.affectedRouteStops
        )

        fun from(item: AbsenceListItem) = AbsenceResponse(
            id = item.id.value,
            childId = item.childId.value,
            type = item.absenceType,
            startDate = item.startDate,
            endDate = item.endDate,
            scheduleId = item.scheduleId?.value,
            scheduleName = item.scheduleName,
            reason = item.reason,
            status = item.status,
            createdByRole = item.createdByRole,
            createdAt = item.createdAt,
            cancelledAt = item.cancelledAt,
            cancellationReason = item.cancellationReason,
            affectedRouteStops = null
        )

        fun from(detail: AbsenceDetail) = AbsenceResponse(
            id = detail.id.value,
            childId = detail.childId.value,
            type = detail.absenceType,
            startDate = detail.startDate,
            endDate = detail.endDate,
            scheduleId = detail.scheduleId?.value,
            scheduleName = detail.scheduleName,
            reason = detail.reason,
            status = detail.status,
            createdByRole = detail.createdByRole,
            createdAt = detail.createdAt,
            cancelledAt = detail.cancelledAt,
            cancellationReason = detail.cancellationReason,
            affectedRouteStops = null
        )
    }
}

data class AbsenceListResponse(
    val absences: List<AbsenceResponse>
) {
    companion object {
        fun from(absences: List<AbsenceListItem>) = AbsenceListResponse(
            absences = absences.map { AbsenceResponse.from(it) }
        )
    }
}

data class CancelAbsenceRequest(
    @field:NotNull(message = "Cancellation reason is required")
    @field:Size(min = 1, max = 1000)
    val reason: String
) {
    fun toCommand(companyId: CompanyId, id: ChildAbsenceId) = CancelAbsenceCommand(
        companyId = companyId,
        id = id,
        reason = reason
    )
}

data class CancelAbsenceResponse(
    val id: String,
    val cancelledAt: Instant,
    val message: String,
    val affectedRouteStops: List<String>
) {
    companion object {
        fun from(result: CancelAbsenceResult) = CancelAbsenceResponse(
            id = result.id.value,
            cancelledAt = result.cancelledAt,
            message = if (result.affectedRouteStopIds.isNotEmpty()) {
                "Absence cancelled. ${result.affectedRouteStopIds.size} route stops were affected and require manual review."
            } else {
                "Absence cancelled successfully."
            },
            affectedRouteStops = result.affectedRouteStopIds
        )
    }
}

data class AbsenceStatsResponse(
    val childId: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val periodFrom: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val periodTo: LocalDate,
    val totalAbsences: Int,
    val totalDays: Int,
    val byType: Map<String, Int>,
    val byStatus: Map<String, Int>
) {
    companion object {
        fun from(stats: AbsenceStats) = AbsenceStatsResponse(
            childId = stats.childId.value,
            periodFrom = stats.periodFrom,
            periodTo = stats.periodTo,
            totalAbsences = stats.totalAbsences,
            totalDays = stats.totalDays,
            byType = stats.byType.mapKeys { it.key.name },
            byStatus = stats.byStatus.mapKeys { it.key.name }
        )
    }
}