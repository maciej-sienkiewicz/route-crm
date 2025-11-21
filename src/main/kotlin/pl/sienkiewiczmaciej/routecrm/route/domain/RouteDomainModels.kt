// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/RouteDomainModels.kt
package pl.sienkiewiczmaciej.routecrm.route.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@JvmInline
value class RouteId(val value: String) {
    companion object {
        fun generate() = RouteId("RT-${UUID.randomUUID()}")
        fun from(value: String) = RouteId(value)
    }
}

enum class RouteStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class Route(
    val id: RouteId,
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val status: RouteStatus,
    val actualStartTime: Instant? = null,
    val actualEndTime: Instant? = null,
    val seriesId: RouteSeriesId? = null,           // ← DODANE
    val seriesOccurrenceDate: LocalDate? = null,   // ← DODANE
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            companyId: CompanyId,
            routeName: String,
            date: LocalDate,
            driverId: DriverId,
            vehicleId: VehicleId,
            estimatedStartTime: LocalTime,
            estimatedEndTime: LocalTime,
            seriesId: RouteSeriesId? = null,           // ← DODANE
            seriesOccurrenceDate: LocalDate? = null    // ← DODANE
        ): Route {
            require(routeName.isNotBlank()) { "Route name is required" }
            require(routeName.length <= 255) { "Route name too long" }
            require(estimatedEndTime.isAfter(estimatedStartTime)) {
                "End time must be after start time"
            }

            return Route(
                id = RouteId.generate(),
                companyId = companyId,
                routeName = routeName,
                date = date,
                driverId = driverId,
                vehicleId = vehicleId,
                estimatedStartTime = estimatedStartTime,
                estimatedEndTime = estimatedEndTime,
                status = RouteStatus.PLANNED,
                seriesId = seriesId,                    // ← DODANE
                seriesOccurrenceDate = seriesOccurrenceDate  // ← DODANE
            )
        }
    }

    fun start(timestamp: Instant): Route {
        require(status == RouteStatus.PLANNED) {
            "Route must be in PLANNED status to start"
        }
        return copy(
            status = RouteStatus.IN_PROGRESS,
            actualStartTime = timestamp
        )
    }

    fun complete(timestamp: Instant): Route {
        require(status == RouteStatus.IN_PROGRESS) {
            "Route must be in IN_PROGRESS status to complete"
        }
        require(actualStartTime != null) { "Route must have been started" }
        require(timestamp.isAfter(actualStartTime)) {
            "Completion time must be after start time"
        }
        return copy(
            status = RouteStatus.COMPLETED,
            actualEndTime = timestamp
        )
    }

    fun cancel(): Route {
        require(status in listOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS)) {
            "Only PLANNED or IN_PROGRESS routes can be cancelled"
        }
        return copy(status = RouteStatus.CANCELLED)
    }

    fun canDeleteStops(): Boolean {
        return status == RouteStatus.PLANNED
    }
}

@JvmInline
value class RouteStopId(val value: String) {
    companion object {
        fun generate() = RouteStopId("ST-${UUID.randomUUID()}")
        fun from(value: String) = RouteStopId(value)
    }
}

enum class StopType {
    PICKUP,
    DROPOFF
}

enum class ExecutionStatus {
    COMPLETED,
    NO_SHOW,
    REFUSED
}

data class RouteStop(
    val id: RouteStopId,
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopOrder: Int,
    val stopType: StopType,
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress,
    val isCancelled: Boolean,
    val cancelledAt: Instant?,
    val cancellationReason: String?,
    val actualTime: Instant?,
    val executionStatus: ExecutionStatus?,
    val executionNotes: String?,
    val executedByUserId: String?,
    val executedByName: String?
) {
    companion object {
        fun create(
            companyId: CompanyId,
            routeId: RouteId,
            stopOrder: Int,
            stopType: StopType,
            childId: ChildId,
            scheduleId: ScheduleId,
            estimatedTime: LocalTime,
            address: ScheduleAddress
        ): RouteStop {
            require(stopOrder > 0) { "Stop order must be positive" }

            return RouteStop(
                id = RouteStopId.generate(),
                companyId = companyId,
                routeId = routeId,
                stopOrder = stopOrder,
                stopType = stopType,
                childId = childId,
                scheduleId = scheduleId,
                estimatedTime = estimatedTime,
                address = address,
                isCancelled = false,
                cancelledAt = null,
                cancellationReason = null,
                actualTime = null,
                executionStatus = null,
                executionNotes = null,
                executedByUserId = null,
                executedByName = null
            )
        }
    }

    fun cancel(reason: String): RouteStop {
        require(!isCancelled) { "Stop already cancelled" }
        require(!isExecuted()) { "Cannot cancel executed stop" }
        return copy(
            isCancelled = true,
            cancelledAt = Instant.now(),
            cancellationReason = reason
        )
    }

    fun updateOrder(newOrder: Int): RouteStop {
        require(!isCancelled) { "Cannot reorder cancelled stop" }
        require(!isExecuted()) { "Cannot reorder executed stop" }
        return copy(stopOrder = newOrder)
    }

    fun updateDetails(estimatedTime: LocalTime, address: ScheduleAddress): RouteStop {
        require(!isCancelled) { "Cannot update cancelled stop" }
        require(!isExecuted()) { "Cannot update executed stop" }
        return copy(
            estimatedTime = estimatedTime,
            address = address
        )
    }

    fun execute(
        actualTime: Instant,
        status: ExecutionStatus,
        executedByUserId: String,
        executedByName: String,
        notes: String?
    ): RouteStop {
        require(!isCancelled) { "Cannot execute cancelled stop" }
        require(!isExecuted()) { "Stop already executed" }
        require(notes == null || notes.length <= 5000) {
            "Execution notes cannot exceed 5000 characters"
        }

        return copy(
            actualTime = actualTime,
            executionStatus = status,
            executionNotes = notes?.trim(),
            executedByUserId = executedByUserId,
            executedByName = executedByName
        )
    }

    fun isExecuted(): Boolean = actualTime != null

    fun canBeDeleted(): Boolean = !isExecuted()

    fun canBeModified(): Boolean = !isExecuted() && !isCancelled
}

@JvmInline
value class RouteNoteId(val value: String) {
    companion object {
        fun generate() = RouteNoteId("RN-${UUID.randomUUID()}")
        fun from(value: String) = RouteNoteId(value)
    }
}

data class RouteNote(
    val id: RouteNoteId,
    val companyId: CompanyId,
    val routeId: RouteId,
    val authorUserId: String,
    val authorName: String,
    val content: String,
    val createdAt: Instant
) {
    companion object {
        fun create(
            companyId: CompanyId,
            routeId: RouteId,
            authorUserId: String,
            authorName: String,
            content: String
        ): RouteNote {
            require(content.isNotBlank()) { "Note content is required" }
            require(content.length <= 5000) { "Note content cannot exceed 5000 characters" }

            return RouteNote(
                id = RouteNoteId.generate(),
                companyId = companyId,
                routeId = routeId,
                authorUserId = authorUserId,
                authorName = authorName,
                content = content.trim(),
                createdAt = Instant.now()
            )
        }
    }
}