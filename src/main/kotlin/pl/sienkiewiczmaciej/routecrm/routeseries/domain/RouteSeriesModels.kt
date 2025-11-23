// routeseries/domain/RouteSeriesModels.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@JvmInline
value class RouteSeriesId(val value: String) {
    companion object {
        fun generate() = RouteSeriesId("RS-${UUID.randomUUID()}")
        fun from(value: String) = RouteSeriesId(value)
    }
}

@JvmInline
value class RouteSeriesScheduleId(val value: String) {
    companion object {
        fun generate() = RouteSeriesScheduleId("RSS-${UUID.randomUUID()}")
        fun from(value: String) = RouteSeriesScheduleId(value)
    }
}

@JvmInline
value class RouteSeriesOccurrenceId(val value: String) {
    companion object {
        fun generate() = RouteSeriesOccurrenceId("RSO-${UUID.randomUUID()}")
        fun from(value: String) = RouteSeriesOccurrenceId(value)
    }
}

enum class RouteSeriesStatus {
    ACTIVE,
    CANCELLED
}

enum class OccurrenceStatus {
    PLANNED,
    MATERIALIZED,
    SKIPPED,
    CANCELLED
}

// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/domain/RouteSeriesModels.kt

data class RouteSeries(
    val id: RouteSeriesId,
    val companyId: CompanyId,
    val seriesName: String,
    val routeNameTemplate: String,
    val driverId: DriverId?, // ← ZMIENIONE: nullable
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val recurrenceInterval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: RouteSeriesStatus,
    val cancelledAt: Instant? = null,
    val cancelledBy: UserId? = null,
    val cancellationReason: String? = null,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            companyId: CompanyId,
            seriesName: String,
            routeNameTemplate: String,
            driverId: DriverId?, // ← ZMIENIONE: nullable
            vehicleId: VehicleId,
            estimatedStartTime: LocalTime,
            estimatedEndTime: LocalTime,
            recurrenceInterval: Int,
            startDate: LocalDate,
            endDate: LocalDate?
        ): RouteSeries {
            require(seriesName.isNotBlank()) { "Series name is required" }
            require(seriesName.length <= 255) { "Series name too long" }
            require(routeNameTemplate.isNotBlank()) { "Route name template is required" }
            require(recurrenceInterval in 1..4) {
                "Recurrence interval must be between 1 and 4 weeks"
            }
            require(estimatedEndTime.isAfter(estimatedStartTime)) {
                "End time must be after start time"
            }
            if (endDate != null) {
                require(endDate.isAfter(startDate)) {
                    "End date must be after start date"
                }
            }

            return RouteSeries(
                id = RouteSeriesId.generate(),
                companyId = companyId,
                seriesName = seriesName,
                routeNameTemplate = routeNameTemplate,
                driverId = driverId, // ← ZMIENIONE: może być null
                vehicleId = vehicleId,
                estimatedStartTime = estimatedStartTime,
                estimatedEndTime = estimatedEndTime,
                recurrenceInterval = recurrenceInterval,
                startDate = startDate,
                endDate = endDate,
                status = RouteSeriesStatus.ACTIVE
            )
        }
    }

    fun matchesRecurrencePattern(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        if (endDate != null && date.isAfter(endDate)) return false
        if (date.dayOfWeek != startDate.dayOfWeek) return false

        val weeksSinceStart = java.time.temporal.ChronoUnit.WEEKS.between(startDate, date)
        return weeksSinceStart % recurrenceInterval == 0L
    }

    fun formatRouteName(date: LocalDate): String {
        return routeNameTemplate.replace("{DATE}", date.toString())
    }

    fun cancel(userId: UserId, reason: String): RouteSeries {
        require(status == RouteSeriesStatus.ACTIVE) {
            "Can only cancel ACTIVE series"
        }
        require(reason.isNotBlank()) { "Cancellation reason is required" }
        require(reason.length <= 5000) { "Cancellation reason too long" }

        return copy(
            status = RouteSeriesStatus.CANCELLED,
            cancelledAt = Instant.now(),
            cancelledBy = userId,
            cancellationReason = reason.trim()
        )
    }
}

data class RouteSeriesSchedule(
    val id: RouteSeriesScheduleId,
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val pickupStopOrder: Int,
    val dropoffStopOrder: Int,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val addedAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            companyId: CompanyId,
            seriesId: RouteSeriesId,
            scheduleId: ScheduleId,
            childId: ChildId,
            pickupStopOrder: Int,
            dropoffStopOrder: Int,
            validFrom: LocalDate,
            validTo: LocalDate?
        ): RouteSeriesSchedule {
            require(pickupStopOrder > 0) { "Pickup stop order must be positive" }
            require(dropoffStopOrder > 0) { "Dropoff stop order must be positive" }
            require(pickupStopOrder < dropoffStopOrder) {
                "Pickup must be before dropoff"
            }
            if (validTo != null) {
                require(validTo.isAfter(validFrom)) {
                    "Valid to must be after valid from"
                }
            }

            return RouteSeriesSchedule(
                id = RouteSeriesScheduleId.generate(),
                companyId = companyId,
                seriesId = seriesId,
                scheduleId = scheduleId,
                childId = childId,
                pickupStopOrder = pickupStopOrder,
                dropoffStopOrder = dropoffStopOrder,
                validFrom = validFrom,
                validTo = validTo
            )
        }
    }

    fun isActiveOn(date: LocalDate): Boolean {
        return !date.isBefore(validFrom) &&
                (validTo == null || !date.isAfter(validTo))
    }

    fun endValidity(endDate: LocalDate): RouteSeriesSchedule {
        require(endDate.isAfter(validFrom)) {
            "End date must be after start date"
        }
        return copy(validTo = endDate)
    }
}

data class RouteSeriesOccurrence(
    val id: RouteSeriesOccurrenceId,
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val occurrenceDate: LocalDate,
    val routeId: RouteId?,
    val status: OccurrenceStatus,
    val materializedAt: Instant?
) {
    companion object {
        fun planned(
            companyId: CompanyId,
            seriesId: RouteSeriesId,
            occurrenceDate: LocalDate
        ) = RouteSeriesOccurrence(
            id = RouteSeriesOccurrenceId.generate(),
            companyId = companyId,
            seriesId = seriesId,
            occurrenceDate = occurrenceDate,
            routeId = null,
            status = OccurrenceStatus.PLANNED,
            materializedAt = null
        )

        fun materialized(
            companyId: CompanyId,
            seriesId: RouteSeriesId,
            occurrenceDate: LocalDate,
            routeId: RouteId
        ) = RouteSeriesOccurrence(
            id = RouteSeriesOccurrenceId.generate(),
            companyId = companyId,
            seriesId = seriesId,
            occurrenceDate = occurrenceDate,
            routeId = routeId,
            status = OccurrenceStatus.MATERIALIZED,
            materializedAt = Instant.now()
        )
    }
}