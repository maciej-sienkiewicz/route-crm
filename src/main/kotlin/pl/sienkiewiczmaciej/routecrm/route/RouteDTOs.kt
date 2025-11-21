// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/RouteDTOs.kt
package pl.sienkiewiczmaciej.routecrm.route

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import pl.sienkiewiczmaciej.routecrm.child.TransportNeedsResponse
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.DisabilityType
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.addschedule.AddRouteScheduleCommand
import pl.sienkiewiczmaciej.routecrm.route.addschedule.AddRouteScheduleResult
import pl.sienkiewiczmaciej.routecrm.route.addschedule.RouteStopCreationData
import pl.sienkiewiczmaciej.routecrm.route.addstop.AddRouteStopCommand
import pl.sienkiewiczmaciej.routecrm.route.availablechildren.AvailableChildItem
import pl.sienkiewiczmaciej.routecrm.route.cancelschedule.CancelRouteScheduleCommand
import pl.sienkiewiczmaciej.routecrm.route.cancelschedule.CancelRouteScheduleResult
import pl.sienkiewiczmaciej.routecrm.route.cancelstop.CancelRouteStopCommand
import pl.sienkiewiczmaciej.routecrm.route.cancelstop.CancelRouteStopResult
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteCommand
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteResult
import pl.sienkiewiczmaciej.routecrm.route.create.RouteStopData
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.executestop.ExecuteRouteStopCommand
import pl.sienkiewiczmaciej.routecrm.route.executestop.ExecuteRouteStopResult
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteDetail
import pl.sienkiewiczmaciej.routecrm.route.list.RouteListItem
import pl.sienkiewiczmaciej.routecrm.route.note.AddNoteResult
import pl.sienkiewiczmaciej.routecrm.route.reassigndriver.ReassignDriverCommand
import pl.sienkiewiczmaciej.routecrm.route.reassigndriver.ReassignDriverResult
import pl.sienkiewiczmaciej.routecrm.route.reorderstops.ReorderRouteStopsCommand
import pl.sienkiewiczmaciej.routecrm.route.reorderstops.ReorderRouteStopsResult
import pl.sienkiewiczmaciej.routecrm.route.reorderstops.StopOrderUpdate
import pl.sienkiewiczmaciej.routecrm.route.updatestatus.UpdateStatusResult
import pl.sienkiewiczmaciej.routecrm.route.updatestop.UpdateRouteStopCommand
import pl.sienkiewiczmaciej.routecrm.route.updatestop.UpdateRouteStopResult
import pl.sienkiewiczmaciej.routecrm.schedule.ScheduleAddressRequest
import pl.sienkiewiczmaciej.routecrm.schedule.ScheduleAddressResponse
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period

data class RouteStopRequest(
    @field:NotNull(message = "Stop order is required")
    @field:Min(1, message = "Stop order must be at least 1")
    val stopOrder: Int,

    @field:NotNull(message = "Stop type is required")
    val stopType: StopType,

    @field:NotBlank(message = "Child ID is required")
    val childId: String,

    @field:NotBlank(message = "Schedule ID is required")
    val scheduleId: String,

    @field:NotNull(message = "Estimated time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Address is required")
    val address: ScheduleAddressRequest
) {
    fun toData() = RouteStopData(
        stopOrder = stopOrder,
        stopType = stopType,
        childId = ChildId.from(childId),
        scheduleId = ScheduleId.from(scheduleId),
        estimatedTime = estimatedTime,
        address = ScheduleAddress(
            label = address.label,
            address = address.toDomainAddress(),
            latitude = null,
            longitude = null
        )
    )
}

data class CreateRouteRequest(
    @field:NotBlank(message = "Route name is required")
    @field:Size(min = 1, max = 255)
    val routeName: String,

    @field:NotNull(message = "Date is required")
    val date: LocalDate,

    @field:NotBlank(message = "Driver ID is required")
    val driverId: String,

    @field:NotBlank(message = "Vehicle ID is required")
    val vehicleId: String,

    @field:NotNull(message = "Estimated start time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,

    @field:NotNull(message = "Estimated end time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,

    @field:Valid
    @field:NotEmpty(message = "At least one stop is required")
    val stops: List<RouteStopRequest>
) {
    fun toCommand(companyId: CompanyId) = CreateRouteCommand(
        companyId = companyId,
        routeName = routeName,
        date = date,
        driverId = DriverId.from(driverId),
        vehicleId = VehicleId.from(vehicleId),
        estimatedStartTime = estimatedStartTime,
        estimatedEndTime = estimatedEndTime,
        stops = stops.map { it.toData() }
    )
}

data class RouteResponse(
    val id: String,
    val companyId: String,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: String?,
    val vehicleId: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val stopsCount: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(result: CreateRouteResult) = RouteResponse(
            id = result.id.value,
            companyId = result.companyId.value,
            routeName = result.routeName,
            date = result.date,
            status = result.status,
            driverId = result.driverId?.value,
            vehicleId = result.vehicleId.value,
            estimatedStartTime = result.estimatedStartTime,
            estimatedEndTime = result.estimatedEndTime,
            actualStartTime = null,
            actualEndTime = null,
            stopsCount = result.stopsCount,
            createdAt = Instant.now()
        )
    }
}

data class DriverSimpleResponse(
    val id: String,
    val firstName: String,
    val lastName: String
)

data class VehicleSimpleResponse(
    val id: String,
    val registrationNumber: String,
    val model: String
)

data class RouteListResponse(
    val id: String,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driver: DriverSimpleResponse,
    val vehicle: VehicleSimpleResponse,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val stopsCount: Int
) {
    companion object {
        fun from(item: RouteListItem) = RouteListResponse(
            id = item.id.value,
            routeName = item.routeName,
            date = item.date,
            status = item.status,
            driver = DriverSimpleResponse(
                id = item.driverId.value,
                firstName = item.driverFirstName,
                lastName = item.driverLastName
            ),
            vehicle = VehicleSimpleResponse(
                id = item.vehicleId.value,
                registrationNumber = item.vehicleRegistrationNumber,
                model = item.vehicleModel
            ),
            estimatedStartTime = item.estimatedStartTime,
            estimatedEndTime = item.estimatedEndTime,
            stopsCount = item.stopsCount
        )
    }
}

data class RouteStopDetailResponse(
    val id: String,
    val stopOrder: Int,
    val stopType: StopType,
    val childId: String,
    val childFirstName: String,
    val childLastName: String,
    val scheduleId: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime,
    val address: ScheduleAddressResponse,
    val isCancelled: Boolean,
    val cancelledAt: Instant?,
    val cancellationReason: String?,
    val actualTime: Instant?,
    val executionStatus: ExecutionStatus?,
    val executionNotes: String?,
    val executedByName: String?,
    val guardian: GuardianSimpleResponse
)

data class GuardianSimpleResponse(
    val firstName: String,
    val lastName: String,
    val phone: String
)

data class RouteNoteResponse(
    val id: String,
    val author: String,
    val content: String,
    val createdAt: Instant
)

data class RouteDetailResponse(
    val id: String,
    val companyId: String,
    val seriesId: String?,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driver: DriverSimpleResponse,
    val vehicle: VehicleSimpleResponse,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val stops: List<RouteStopDetailResponse>,
    val notes: List<RouteNoteResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(detail: RouteDetail) = RouteDetailResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
            seriesId = detail.seriesId?.value,
            routeName = detail.routeName,
            date = detail.date,
            status = detail.status,
            driver = DriverSimpleResponse(
                id = detail.driverId.value,
                firstName = detail.driverFirstName,
                lastName = detail.driverLastName
            ),
            vehicle = VehicleSimpleResponse(
                id = detail.vehicleId.value,
                registrationNumber = detail.vehicleRegistrationNumber,
                model = detail.vehicleModel
            ),
            estimatedStartTime = detail.estimatedStartTime,
            estimatedEndTime = detail.estimatedEndTime,
            actualStartTime = detail.actualStartTime,
            actualEndTime = detail.actualEndTime,
            stops = detail.stops.map { stop ->
                RouteStopDetailResponse(
                    id = stop.id.value,
                    stopOrder = stop.stopOrder,
                    stopType = stop.stopType,
                    childId = stop.childId.value,
                    childFirstName = stop.childFirstName,
                    childLastName = stop.childLastName,
                    scheduleId = stop.scheduleId.value,
                    estimatedTime = stop.estimatedTime,
                    address = ScheduleAddressResponse.from(stop.address),
                    isCancelled = stop.isCancelled,
                    cancelledAt = stop.cancelledAt,
                    cancellationReason = stop.cancellationReason,
                    actualTime = stop.actualTime,
                    executionStatus = stop.executionStatus,
                    executionNotes = stop.executionNotes,
                    executedByName = stop.executedByName,
                    guardian = GuardianSimpleResponse(
                        firstName = stop.guardianFirstName,
                        lastName = stop.guardianLastName,
                        phone = stop.guardianPhone
                    )
                )
            },
            notes = detail.notes.map { note ->
                RouteNoteResponse(
                    id = note.id.value,
                    author = note.authorName,
                    content = note.content,
                    createdAt = note.createdAt
                )
            },
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

data class UpdateRouteStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: RouteStatus,

    val actualStartTime: Instant?,
    val actualEndTime: Instant?
)

data class UpdateRouteStatusResponse(
    val id: String,
    val status: RouteStatus,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateStatusResult) = UpdateRouteStatusResponse(
            id = result.id.value,
            status = result.status,
            actualStartTime = result.actualStartTime,
            actualEndTime = result.actualEndTime,
            updatedAt = Instant.now()
        )
    }
}

data class AddRouteNoteRequest(
    @field:NotBlank(message = "Note content is required")
    @field:Size(max = 5000)
    val content: String
)

data class AddRouteNoteResponse(
    val id: String,
    val routeId: String,
    val author: String,
    val content: String,
    val createdAt: Instant
) {
    companion object {
        fun from(result: AddNoteResult) = AddRouteNoteResponse(
            id = result.id.value,
            routeId = result.routeId.value,
            author = result.authorName,
            content = result.content,
            createdAt = result.createdAt
        )
    }
}

data class AddRouteStopRequest(
    @field:NotNull(message = "Stop order is required")
    @field:Min(1, message = "Stop order must be at least 1")
    val stopOrder: Int,

    @field:NotNull(message = "Stop type is required")
    val stopType: StopType,

    @field:NotBlank(message = "Child ID is required")
    val childId: String,

    @field:NotBlank(message = "Schedule ID is required")
    val scheduleId: String,

    @field:NotNull(message = "Estimated time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Address is required")
    val address: ScheduleAddressRequest
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId) = AddRouteStopCommand(
        companyId = companyId,
        routeId = routeId,
        stopOrder = stopOrder,
        stopType = stopType,
        childId = ChildId.from(childId),
        scheduleId = ScheduleId.from(scheduleId),
        estimatedTime = estimatedTime,
        address = ScheduleAddress(
            label = address.label,
            address = address.toDomainAddress(),
            latitude = null,
            longitude = null
        )
    )
}

data class ReorderStopsRequest(
    @field:NotEmpty(message = "At least one stop order is required")
    val stopOrders: List<StopOrderRequest>
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId) = ReorderRouteStopsCommand(
        companyId = companyId,
        routeId = routeId,
        stopOrders = stopOrders.map {
            StopOrderUpdate(
                stopId = RouteStopId.from(it.stopId),
                newOrder = it.newOrder
            )
        }
    )
}

data class StopOrderRequest(
    @field:NotBlank(message = "Stop ID is required")
    val stopId: String,

    @field:NotNull(message = "New order is required")
    @field:Min(1, message = "Order must be at least 1")
    val newOrder: Int
)

data class ReorderStopsResponse(
    val routeId: String,
    val updatedStopsCount: Int
) {
    companion object {
        fun from(result: ReorderRouteStopsResult) = ReorderStopsResponse(
            routeId = result.routeId.value,
            updatedStopsCount = result.updatedStopsCount
        )
    }
}

data class CancelRouteStopRequest(
    @field:NotBlank(message = "Cancellation reason is required")
    @field:Size(max = 5000)
    val reason: String
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId, stopId: RouteStopId) =
        CancelRouteStopCommand(
            companyId = companyId,
            routeId = routeId,
            stopId = stopId,
            reason = reason
        )
}

data class CancelRouteStopResponse(
    val id: String,
    val isCancelled: Boolean,
    val cancelledAt: Instant?,
    val cancellationReason: String?
) {
    companion object {
        fun from(result: CancelRouteStopResult) = CancelRouteStopResponse(
            id = result.id.value,
            isCancelled = result.isCancelled,
            cancelledAt = result.cancelledAt,
            cancellationReason = result.cancellationReason
        )
    }
}

data class ExecuteRouteStopRequest(
    @field:NotNull(message = "Actual time is required")
    val actualTime: Instant,

    @field:NotNull(message = "Execution status is required")
    val status: ExecutionStatus,

    @field:Size(max = 5000)
    val notes: String?
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId, stopId: RouteStopId) =
        ExecuteRouteStopCommand(
            companyId = companyId,
            routeId = routeId,
            stopId = stopId,
            actualTime = actualTime,
            status = status,
            notes = notes
        )
}

data class ExecuteRouteStopResponse(
    val id: String,
    val actualTime: Instant,
    val executionStatus: ExecutionStatus,
    val executedByName: String
) {
    companion object {
        fun from(result: ExecuteRouteStopResult) = ExecuteRouteStopResponse(
            id = result.id.value,
            actualTime = result.actualTime,
            executionStatus = result.executionStatus,
            executedByName = result.executedByName
        )
    }
}

data class AvailableChildResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeedsResponse,
    val schedule: AvailableChildScheduleResponse,
    val guardian: AvailableChildGuardianResponse
) {
    companion object {
        fun from(item: AvailableChildItem, referenceDate: LocalDate): AvailableChildResponse {
            val age = Period.between(item.birthDate, referenceDate).years

            return AvailableChildResponse(
                id = item.childId.value,
                firstName = item.firstName,
                lastName = item.lastName,
                age = age,
                disability = item.disability,
                transportNeeds = TransportNeedsResponse.from(item.transportNeeds),
                schedule = AvailableChildScheduleResponse(
                    id = item.scheduleId.value,
                    name = item.scheduleName,
                    pickupTime = item.pickupTime,
                    dropoffTime = item.dropoffTime,
                    pickupAddress = ScheduleAddressResponse.from(item.pickupAddress),
                    dropoffAddress = ScheduleAddressResponse.from(item.dropoffAddress)
                ),
                guardian = AvailableChildGuardianResponse(
                    firstName = item.guardianFirstName,
                    lastName = item.guardianLastName,
                    phone = item.guardianPhone
                )
            )
        }
    }
}

data class AvailableChildScheduleResponse(
    val id: String,
    val name: String,
    @JsonFormat(pattern = "HH:mm")
    val pickupTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val dropoffTime: LocalTime,
    val pickupAddress: ScheduleAddressResponse,
    val dropoffAddress: ScheduleAddressResponse
)

data class AvailableChildGuardianResponse(
    val firstName: String,
    val lastName: String,
    val phone: String
)

data class UpdateRouteStopRequest(
    @field:NotNull(message = "Estimated time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Address is required")
    val address: ScheduleAddressRequest
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId, stopId: RouteStopId) =
        UpdateRouteStopCommand(
            companyId = companyId,
            routeId = routeId,
            stopId = stopId,
            estimatedTime = estimatedTime,
            address = ScheduleAddress(
                label = address.label,
                address = address.toDomainAddress(),
                latitude = null, // Będzie uzupełnione przez handler
                longitude = null
            )
        )
}

data class UpdateRouteStopResponse(
    val id: String,
    val estimatedTime: LocalTime,
    val address: ScheduleAddressResponse,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateRouteStopResult) = UpdateRouteStopResponse(
            id = result.id.value,
            estimatedTime = result.estimatedTime,
            address = ScheduleAddressResponse.from(result.address),
            updatedAt = Instant.now()
        )
    }
}

data class AddRouteScheduleRequest(
    @field:NotBlank(message = "Child ID is required")
    val childId: String,

    @field:NotBlank(message = "Schedule ID is required")
    val scheduleId: String,

    @field:Valid
    @field:NotNull(message = "Pickup stop is required")
    val pickupStop: RouteStopDataRequest,

    @field:Valid
    @field:NotNull(message = "Dropoff stop is required")
    val dropoffStop: RouteStopDataRequest
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId) = AddRouteScheduleCommand(
        companyId = companyId,
        routeId = routeId,
        childId = ChildId.from(childId),
        scheduleId = ScheduleId.from(scheduleId),
        pickupStop = pickupStop.toData(StopType.PICKUP),
        dropoffStop = dropoffStop.toData(StopType.DROPOFF)
    )
}

data class RouteStopDataRequest(
    @field:NotNull(message = "Stop order is required")
    @field:Min(1, message = "Stop order must be at least 1")
    val stopOrder: Int,

    @field:NotNull(message = "Estimated time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Address is required")
    val address: ScheduleAddressRequest
) {
    fun toData(stopType: StopType) = RouteStopCreationData(
        stopOrder = stopOrder,
        stopType = stopType,
        estimatedTime = estimatedTime,
        address = ScheduleAddress(
            label = address.label,
            address = address.toDomainAddress(),
            latitude = null,
            longitude = null
        )
    )
}

data class AddRouteScheduleResponse(
    val pickupStopId: String,
    val dropoffStopId: String,
    val scheduleId: String,
    val childId: String
) {
    companion object {
        fun from(result: AddRouteScheduleResult) = AddRouteScheduleResponse(
            pickupStopId = result.pickupStopId.value,
            dropoffStopId = result.dropoffStopId.value,
            scheduleId = result.scheduleId.value,
            childId = result.childId.value
        )
    }
}

data class CancelRouteScheduleRequest(
    @field:NotBlank(message = "Cancellation reason is required")
    @field:Size(max = 5000)
    val reason: String
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId, scheduleId: ScheduleId) =
        CancelRouteScheduleCommand(
            companyId = companyId,
            routeId = routeId,
            scheduleId = scheduleId,
            reason = reason
        )
}

data class CancelRouteScheduleResponse(
    val scheduleId: String,
    val pickupStopId: String,
    val dropoffStopId: String,
    val cancelledStopsCount: Int,
    val cancelledAt: Instant
) {
    companion object {
        fun from(result: CancelRouteScheduleResult) = CancelRouteScheduleResponse(
            scheduleId = result.scheduleId.value,
            pickupStopId = result.pickupStopId.value,
            dropoffStopId = result.dropoffStopId.value,
            cancelledStopsCount = result.cancelledStopsCount,
            cancelledAt = result.cancelledAt
        )
    }
}

data class RouteHistoryResponse(
    val id: String,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driver: DriverSimpleResponse,
    val vehicle: VehicleSimpleResponse,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val stopsCount: Int,
    val completedStopsCount: Int
) {
    companion object {
        fun from(item: pl.sienkiewiczmaciej.routecrm.route.history.RouteHistoryItem) = RouteHistoryResponse(
            id = item.id.value,
            routeName = item.routeName,
            date = item.date,
            status = item.status,
            driver = DriverSimpleResponse(
                id = item.driverId.value,
                firstName = item.driverFirstName,
                lastName = item.driverLastName
            ),
            vehicle = VehicleSimpleResponse(
                id = item.vehicleId.value,
                registrationNumber = item.vehicleRegistrationNumber,
                model = item.vehicleModel
            ),
            estimatedStartTime = item.estimatedStartTime,
            estimatedEndTime = item.estimatedEndTime,
            actualStartTime = item.actualStartTime,
            actualEndTime = item.actualEndTime,
            stopsCount = item.stopsCount,
            completedStopsCount = item.completedStopsCount
        )
    }
}

data class UpcomingRouteResponse(
    val id: String,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driver: DriverSimpleResponse,
    val vehicle: VehicleSimpleResponse,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val stopsCount: Int,
    val childStops: List<ChildStopInfoResponse>
) {
    companion object {
        fun from(item: pl.sienkiewiczmaciej.routecrm.route.upcoming.UpcomingRouteItem) = UpcomingRouteResponse(
            id = item.id.value,
            routeName = item.routeName,
            date = item.date,
            status = item.status,
            driver = DriverSimpleResponse(
                id = item.driverId.value,
                firstName = item.driverFirstName,
                lastName = item.driverLastName
            ),
            vehicle = VehicleSimpleResponse(
                id = item.vehicleId.value,
                registrationNumber = item.vehicleRegistrationNumber,
                model = item.vehicleModel
            ),
            estimatedStartTime = item.estimatedStartTime,
            estimatedEndTime = item.estimatedEndTime,
            stopsCount = item.stopsCount,
            childStops = item.childStops.map { ChildStopInfoResponse.from(it) }
        )
    }
}

data class ChildStopInfoResponse(
    val stopId: String,
    val stopOrder: Int,
    val stopType: StopType,
    val childFirstName: String,
    val childLastName: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime,
    val address: ScheduleAddressResponse
) {
    companion object {
        fun from(stop: pl.sienkiewiczmaciej.routecrm.route.upcoming.ChildStopDetail) = ChildStopInfoResponse(
            stopId = stop.stopId.value,
            stopOrder = stop.stopOrder,
            stopType = stop.stopType,
            childFirstName = stop.childFirstName,
            childLastName = stop.childLastName,
            estimatedTime = stop.estimatedTime,
            address = ScheduleAddressResponse.from(stop.address)
        )
    }
}

// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/RouteDTOs.kt
// Dodaj na końcu istniejącego pliku:

data class RouteSuggestionResponse(
    val id: String,
    val companyId: String,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driver: DriverSimpleResponse,
    val vehicle: VehicleSimpleResponse,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val stops: List<RouteStopSuggestionResponse>
) {
    companion object {
        fun from(detail: pl.sienkiewiczmaciej.routecrm.route.suggestions.RouteSuggestionDetail) = RouteSuggestionResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
            routeName = detail.routeName,
            date = detail.date,
            status = detail.status,
            driver = DriverSimpleResponse(
                id = detail.driver.id.value,
                firstName = detail.driver.firstName,
                lastName = detail.driver.lastName
            ),
            vehicle = VehicleSimpleResponse(
                id = detail.vehicle.id.value,
                registrationNumber = detail.vehicle.registrationNumber,
                model = detail.vehicle.model
            ),
            estimatedStartTime = detail.estimatedStartTime,
            estimatedEndTime = detail.estimatedEndTime,
            actualStartTime = detail.actualStartTime,
            actualEndTime = detail.actualEndTime,
            stops = detail.stops.map { RouteStopSuggestionResponse.from(it) }
        )
    }
}

data class RouteStopSuggestionResponse(
    val id: String,
    val stopOrder: Int,
    val stopType: StopType,
    val childId: String,
    val childFirstName: String,
    val childLastName: String,
    val scheduleId: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime,
    val address: ScheduleAddressResponse,
    val isCancelled: Boolean,
    val actualTime: Instant?,
    val executionStatus: ExecutionStatus?,
    val guardian: GuardianSimpleResponse
) {
    companion object {
        fun from(stop: pl.sienkiewiczmaciej.routecrm.route.suggestions.RouteStopSimple) = RouteStopSuggestionResponse(
            id = stop.id.value,
            stopOrder = stop.stopOrder,
            stopType = stop.stopType,
            childId = stop.childId.value,
            childFirstName = stop.childFirstName,
            childLastName = stop.childLastName,
            scheduleId = stop.scheduleId.value,
            estimatedTime = stop.estimatedTime,
            address = ScheduleAddressResponse.from(stop.address),
            isCancelled = stop.isCancelled,
            actualTime = stop.actualTime,
            executionStatus = stop.executionStatus,
            guardian = GuardianSimpleResponse(
                firstName = stop.guardian.firstName,
                lastName = stop.guardian.lastName,
                phone = stop.guardian.phone
            )
        )
    }
}

data class ReassignDriverRequest(
    @field:NotBlank(message = "New driver ID is required")
    val newDriverId: String,

    @field:Size(max = 5000)
    val reason: String? = null
) {
    fun toCommand(companyId: CompanyId, routeId: RouteId) =
        ReassignDriverCommand(
            companyId = companyId,
            routeId = routeId,
            newDriverId = DriverId.from(newDriverId),
            reason = reason
        )
}

data class ReassignDriverResponse(
    val routeId: String,
    val previousDriverId: String,
    val newDriverId: String,
    val status: RouteStatus,
    val assignmentId: String,
    val message: String
) {
    companion object {
        fun from(result: ReassignDriverResult) = ReassignDriverResponse(
            routeId = result.routeId.value,
            previousDriverId = result.previousDriverId.value,
            newDriverId = result.newDriverId.value,
            status = result.status,
            assignmentId = result.assignmentId.value,
            message = "Driver reassigned successfully")
    }
}

data class DriverAssignmentHistoryResponse(
    val id: String,
    val previousDriverId: String,
    val previousDriverName: String,
    val newDriverId: String,
    val newDriverName: String,
    val reassignedBy: String,
    val reassignedByName: String,
    val reassignedAt: Instant,
    val reason: String?
)