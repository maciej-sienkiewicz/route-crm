// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/RouteSeriesDTOs.kt
package pl.sienkiewiczmaciej.routecrm.routeseries

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.routeseries.addchild.AddChildToRouteSeriesCommand
import pl.sienkiewiczmaciej.routecrm.routeseries.addchild.AddChildToSeriesResult
import pl.sienkiewiczmaciej.routecrm.routeseries.cancel.CancelRouteSeriesCommand
import pl.sienkiewiczmaciej.routecrm.routeseries.cancel.CancelRouteSeriesResult
import pl.sienkiewiczmaciej.routecrm.routeseries.create.CreateRouteSeriesFromRouteCommand
import pl.sienkiewiczmaciej.routecrm.routeseries.create.CreateSeriesFromRouteResult
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeries
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.removechild.RemoveChildFromRouteSeriesCommand
import pl.sienkiewiczmaciej.routecrm.routeseries.removechild.RemoveChildFromSeriesResult
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class CreateRouteSeriesFromRouteRequest(
    @field:NotBlank val seriesName: String,
    @field:NotNull @field:Min(1) @field:Max(4) val recurrenceInterval: Int,  // 1-4 tygodnie
    @field:NotNull val startDate: LocalDate,
    val endDate: LocalDate?
) {
    fun toCommand(companyId: CompanyId, sourceRouteId: RouteId) =
        CreateRouteSeriesFromRouteCommand(
            companyId = companyId,
            sourceRouteId = sourceRouteId,
            seriesName = seriesName,
            recurrenceInterval = recurrenceInterval,
            startDate = startDate,
            endDate = endDate
        )
}

data class CreateRouteSeriesFromRouteResponse(
    val seriesId: String,
    val seriesName: String,
    val schedulesCount: Int,
    val routesMaterialized: Int
) {
    companion object {
        fun from(result: CreateSeriesFromRouteResult) = CreateRouteSeriesFromRouteResponse(
            seriesId = result.seriesId.value,
            seriesName = result.seriesName,
            schedulesCount = result.schedulesCount,
            routesMaterialized = result.routesMaterialized
        )
    }
}

data class RouteSeriesResponse(
    val id: String,
    val companyId: String,
    val seriesName: String,
    val routeNameTemplate: String,
    val driverId: String,
    val vehicleId: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val recurrenceInterval: Int,
    val dayOfWeek: String,  // "MONDAY", "WEDNESDAY", etc.
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: RouteSeriesStatus,
    val cancelledAt: Instant?,
    val cancelledBy: String?,
    val cancellationReason: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(series: RouteSeries) = RouteSeriesResponse(
            id = series.id.value,
            companyId = series.companyId.value,
            seriesName = series.seriesName,
            routeNameTemplate = series.routeNameTemplate,
            driverId = series.driverId.value,
            vehicleId = series.vehicleId.value,
            estimatedStartTime = series.estimatedStartTime,
            estimatedEndTime = series.estimatedEndTime,
            recurrenceInterval = series.recurrenceInterval,
            dayOfWeek = series.startDate.dayOfWeek.toString(),
            startDate = series.startDate,
            endDate = series.endDate,
            status = series.status,
            cancelledAt = series.cancelledAt,
            cancelledBy = series.cancelledBy?.value,
            cancellationReason = series.cancellationReason,
            createdAt = series.createdAt
        )
    }
}

data class AddChildToSeriesRequest(
    @field:NotBlank val childId: String,
    @field:NotBlank val scheduleId: String,
    @field:NotNull @field:Min(1) val pickupStopOrder: Int,
    @field:NotNull @field:Min(1) val dropoffStopOrder: Int,
    @field:NotNull val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null
) {
    fun toCommand(companyId: CompanyId, seriesId: RouteSeriesId) =
        AddChildToRouteSeriesCommand(
            companyId = companyId,
            seriesId = seriesId,
            scheduleId = pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId.from(scheduleId),
            childId = pl.sienkiewiczmaciej.routecrm.child.domain.ChildId.from(childId),
            pickupStopOrder = pickupStopOrder,
            dropoffStopOrder = dropoffStopOrder,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo
        )
}

data class AddChildToSeriesResponse(
    val seriesId: String,
    val scheduleId: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val existingRoutesUpdated: Int,
    val conflictResolved: Boolean,
    val message: String?
) {
    companion object {
        fun from(result: AddChildToSeriesResult, conflictMessage: String? = null) =
            AddChildToSeriesResponse(
                seriesId = result.seriesId.value,
                scheduleId = result.scheduleId.value,
                effectiveFrom = result.effectiveFrom,
                effectiveTo = result.effectiveTo,
                existingRoutesUpdated = result.existingRoutesUpdated,
                conflictResolved = result.conflictResolved,
                message = conflictMessage
            )
    }
}

data class RemoveChildFromSeriesRequest(
    @field:NotNull val effectiveFrom: LocalDate,
    val cancelExistingStops: Boolean = true
) {
    fun toCommand(companyId: CompanyId, seriesId: RouteSeriesId, scheduleId: pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId) =
        RemoveChildFromRouteSeriesCommand(
            companyId = companyId,
            seriesId = seriesId,
            scheduleId = scheduleId,
            effectiveFrom = effectiveFrom,
            cancelExistingStops = cancelExistingStops
        )
}

data class RemoveChildFromSeriesResponse(
    val seriesId: String,
    val scheduleId: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate,
    val stopsCancelled: Int
) {
    companion object {
        fun from(result: RemoveChildFromSeriesResult) = RemoveChildFromSeriesResponse(
            seriesId = result.seriesId.value,
            scheduleId = result.scheduleId.value,
            effectiveFrom = result.effectiveFrom,
            effectiveTo = result.effectiveTo,
            stopsCancelled = result.stopsCancelled
        )
    }
}

data class CancelRouteSeriesRequest(
    @field:NotBlank val reason: String,
    val cancelFutureRoutes: Boolean = true
) {
    fun toCommand(companyId: CompanyId, seriesId: RouteSeriesId) =
        CancelRouteSeriesCommand(
            companyId = companyId,
            seriesId = seriesId,
            reason = reason,
            cancelFutureRoutes = cancelFutureRoutes
        )
}

data class CancelRouteSeriesResponse(
    val seriesId: String,
    val status: RouteSeriesStatus,
    val futureRoutesCancelled: Int
) {
    companion object {
        fun from(result: CancelRouteSeriesResult) = CancelRouteSeriesResponse(
            seriesId = result.seriesId.value,
            status = result.status,
            futureRoutesCancelled = result.futureRoutesCancelled
        )
    }
}

data class RouteSeriesListResponse(
    val id: String,
    val seriesName: String,
    val recurrenceInterval: Int,
    val dayOfWeek: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: RouteSeriesStatus,
    val schedulesCount: Int
)