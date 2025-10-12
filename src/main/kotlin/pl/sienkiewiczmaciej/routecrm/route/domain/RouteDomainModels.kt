package pl.sienkiewiczmaciej.routecrm.route.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
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

enum class ChildInRouteStatus {
    PENDING,
    IN_VEHICLE,
    DELIVERED,
    ABSENT
}

data class Route(
    val id: RouteId,
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?
) {
    companion object {
        fun create(
            companyId: CompanyId,
            routeName: String,
            date: LocalDate,
            driverId: DriverId,
            vehicleId: VehicleId,
            estimatedStartTime: LocalTime,
            estimatedEndTime: LocalTime
        ): Route {
            require(routeName.isNotBlank()) { "Route name is required" }
            require(routeName.length in 1..255) { "Route name must be between 1 and 255 characters" }
            require(!date.isBefore(LocalDate.now())) { "Route date must be today or in the future" }
            require(estimatedEndTime.isAfter(estimatedStartTime)) {
                "Estimated end time must be after start time"
            }

            return Route(
                id = RouteId.generate(),
                companyId = companyId,
                routeName = routeName.trim(),
                date = date,
                status = RouteStatus.PLANNED,
                driverId = driverId,
                vehicleId = vehicleId,
                estimatedStartTime = estimatedStartTime,
                estimatedEndTime = estimatedEndTime,
                actualStartTime = null,
                actualEndTime = null
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
}

data class RouteChild(
    val id: RouteChildId,
    val companyId: CompanyId,
    val routeId: RouteId,
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val pickupOrder: Int,
    val pickupAddress: ScheduleAddress,
    val dropoffAddress: ScheduleAddress,
    val estimatedPickupTime: LocalTime,
    val estimatedDropoffTime: LocalTime,
    val actualPickupTime: Instant?,
    val actualDropoffTime: Instant?,
    val status: ChildInRouteStatus
) {
    companion object {
        fun create(
            companyId: CompanyId,
            routeId: RouteId,
            childId: ChildId,
            scheduleId: ScheduleId,
            pickupOrder: Int,
            pickupAddress: ScheduleAddress,
            dropoffAddress: ScheduleAddress,
            estimatedPickupTime: LocalTime,
            estimatedDropoffTime: LocalTime
        ): RouteChild {
            require(pickupOrder > 0) { "Pickup order must be positive" }
            require(estimatedDropoffTime.isAfter(estimatedPickupTime)) {
                "Estimated dropoff time must be after pickup time"
            }

            return RouteChild(
                id = RouteChildId.generate(),
                companyId = companyId,
                routeId = routeId,
                childId = childId,
                scheduleId = scheduleId,
                pickupOrder = pickupOrder,
                pickupAddress = pickupAddress,
                dropoffAddress = dropoffAddress,
                estimatedPickupTime = estimatedPickupTime,
                estimatedDropoffTime = estimatedDropoffTime,
                actualPickupTime = null,
                actualDropoffTime = null,
                status = ChildInRouteStatus.PENDING
            )
        }
    }

    fun pickup(timestamp: Instant): RouteChild {
        require(status == ChildInRouteStatus.PENDING) {
            "Child must be in PENDING status to pickup"
        }
        return copy(
            status = ChildInRouteStatus.IN_VEHICLE,
            actualPickupTime = timestamp
        )
    }

    fun deliver(timestamp: Instant): RouteChild {
        require(status == ChildInRouteStatus.IN_VEHICLE) {
            "Child must be in IN_VEHICLE status to deliver"
        }
        require(actualPickupTime != null) { "Child must have been picked up" }
        require(timestamp.isAfter(actualPickupTime)) {
            "Delivery time must be after pickup time"
        }
        return copy(
            status = ChildInRouteStatus.DELIVERED,
            actualDropoffTime = timestamp
        )
    }

    fun markAbsent(): RouteChild {
        require(status == ChildInRouteStatus.PENDING) {
            "Only PENDING children can be marked absent"
        }
        return copy(status = ChildInRouteStatus.ABSENT)
    }
}

@JvmInline
value class RouteChildId(val value: String) {
    companion object {
        fun generate() = RouteChildId("RC-${UUID.randomUUID()}")
        fun from(value: String) = RouteChildId(value)
    }
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