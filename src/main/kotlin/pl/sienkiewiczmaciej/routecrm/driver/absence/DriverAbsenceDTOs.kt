// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/DriverAbsenceDTOs.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.driver.absence.cancel.CancelDriverAbsenceCommand
import pl.sienkiewiczmaciej.routecrm.driver.absence.cancel.CancelDriverAbsenceResult
import pl.sienkiewiczmaciej.routecrm.driver.absence.create.CreateDriverAbsenceCommand
import pl.sienkiewiczmaciej.routecrm.driver.absence.create.CreateDriverAbsenceResult
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceType
import pl.sienkiewiczmaciej.routecrm.driver.absence.getbyid.DriverAbsenceDetail
import pl.sienkiewiczmaciej.routecrm.driver.absence.list.DriverAbsenceListItem
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant
import java.time.LocalDate

data class CreateDriverAbsenceRequest(
    @field:NotNull(message = "Absence type is required")
    val type: DriverAbsenceType,

    @field:NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,

    @field:NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate,

    @field:Size(max = 1000)
    val reason: String?
) {
    fun toCommand(companyId: CompanyId, driverId: DriverId) = CreateDriverAbsenceCommand(
        companyId = companyId,
        driverId = driverId,
        absenceType = type,
        startDate = startDate,
        endDate = endDate,
        reason = reason
    )
}

data class DriverAbsenceResponse(
    val id: String,
    val driverId: String,
    val type: DriverAbsenceType,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate,
    val reason: String?,
    val status: DriverAbsenceStatus,
    val createdByRole: UserRole,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val cancellationReason: String?,
    val conflictingRoutesCount: Int?
) {
    companion object {
        fun from(result: CreateDriverAbsenceResult) = DriverAbsenceResponse(
            id = result.id.value,
            driverId = result.driverId.value,
            type = result.absenceType,
            startDate = result.startDate,
            endDate = result.endDate,
            reason = result.reason,
            status = result.status,
            createdByRole = UserRole.ADMIN,
            createdAt = Instant.now(),
            cancelledAt = null,
            cancellationReason = null,
            conflictingRoutesCount = result.conflictingRoutesCount
        )

        fun fromDetail(detail: DriverAbsenceDetail) = DriverAbsenceResponse(
            id = detail.id.value,
            driverId = detail.driverId.value,
            type = detail.absenceType,
            startDate = detail.startDate,
            endDate = detail.endDate,
            reason = detail.reason,
            status = detail.status,
            createdByRole = detail.createdByRole,
            createdAt = detail.createdAt,
            cancelledAt = detail.cancelledAt,
            cancellationReason = detail.cancellationReason,
            conflictingRoutesCount = null
        )

        fun from(item: DriverAbsenceListItem) = DriverAbsenceResponse(
            id = item.id.value,
            driverId = item.driverId.value,
            type = item.absenceType,
            startDate = item.startDate,
            endDate = item.endDate,
            reason = item.reason,
            status = item.status,
            createdByRole = item.createdByRole,
            createdAt = item.createdAt,
            cancelledAt = item.cancelledAt,
            cancellationReason = item.cancellationReason,
            conflictingRoutesCount = null
        )
    }
}

data class DriverAbsenceListResponse(
    val absences: List<DriverAbsenceResponse>
) {
    companion object {
        fun from(absences: List<DriverAbsenceListItem>) = DriverAbsenceListResponse(
            absences = absences.map { DriverAbsenceResponse.from(it) }
        )
    }
}

data class CancelDriverAbsenceRequest(
    @field:NotNull(message = "Cancellation reason is required")
    @field:Size(min = 1, max = 1000)
    val reason: String
) {
    fun toCommand(companyId: CompanyId, id: DriverAbsenceId) = CancelDriverAbsenceCommand(
        companyId = companyId,
        id = id,
        reason = reason
    )
}

data class CancelDriverAbsenceResponse(
    val id: String,
    val cancelledAt: Instant,
    val message: String
) {
    companion object {
        fun from(result: CancelDriverAbsenceResult) = CancelDriverAbsenceResponse(
            id = result.id.value,
            cancelledAt = result.cancelledAt,
            message = "Driver absence cancelled successfully"
        )
    }
}