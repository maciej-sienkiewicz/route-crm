package pl.sienkiewiczmaciej.routecrm.route

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.child.TransportNeedsResponse
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.DisabilityType
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.availablechildren.AvailableChildItem
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteCommand
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteResult
import pl.sienkiewiczmaciej.routecrm.route.create.RouteChildData
import pl.sienkiewiczmaciej.routecrm.route.domain.ChildInRouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteDetail
import pl.sienkiewiczmaciej.routecrm.route.list.RouteListItem
import pl.sienkiewiczmaciej.routecrm.route.note.AddNoteResult
import pl.sienkiewiczmaciej.routecrm.route.updatechildstatus.UpdateChildStatusResult
import pl.sienkiewiczmaciej.routecrm.route.updatestatus.UpdateStatusResult
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

data class RouteChildRequest(
    @field:NotBlank(message = "Child ID is required")
    val childId: String,

    @field:NotBlank(message = "Schedule ID is required")
    val scheduleId: String,

    @field:NotNull(message = "Pickup order is required")
    @field:Min(1, message = "Pickup order must be at least 1")
    val pickupOrder: Int,

    @field:Valid
    @field:NotNull(message = "Pickup address is required")
    val pickupAddress: ScheduleAddressRequest,

    @field:Valid
    @field:NotNull(message = "Dropoff address is required")
    val dropoffAddress: ScheduleAddressRequest,

    @field:NotNull(message = "Estimated pickup time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedPickupTime: LocalTime,

    @field:NotNull(message = "Estimated dropoff time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedDropoffTime: LocalTime
) {
    fun toData() = RouteChildData(
        childId = ChildId.from(childId),
        scheduleId = ScheduleId.from(scheduleId),
        pickupOrder = pickupOrder,
        // Budujemy pełny ScheduleAddress (bez współrzędnych - zostaną skopiowane z harmonogramu)
        pickupAddress = ScheduleAddress(
            label = pickupAddress.label,
            address = pickupAddress.toDomainAddress(),
            latitude = null,  // Będą pobrane z harmonogramu
            longitude = null
        ),
        dropoffAddress = ScheduleAddress(
            label = dropoffAddress.label,
            address = dropoffAddress.toDomainAddress(),
            latitude = null,  // Będą pobrane z harmonogramu
            longitude = null
        ),
        estimatedPickupTime = estimatedPickupTime,
        estimatedDropoffTime = estimatedDropoffTime
    )
}

enum class RoutePointType {
    PICKUP,
    DROPOFF
}

data class RoutePointRequest(
    @field:NotBlank(message = "Child ID is required")
    val childId: String,

    @field:NotBlank(message = "Schedule ID is required")
    val scheduleId: String,

    @field:NotNull(message = "Point type is required")
    val type: RoutePointType,

    @field:NotNull(message = "Order is required")
    @field:Min(1, message = "Order must be at least 1")
    val order: Int,

    @field:NotNull(message = "Estimated time is required")
    @JsonFormat(pattern = "HH:mm")
    val estimatedTime: LocalTime
)

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
    @field:NotEmpty(message = "At least one point is required")
    val points: List<RoutePointRequest>
) {
    fun toCommand(companyId: CompanyId) = CreateRouteCommand(
        companyId = companyId,
        routeName = routeName,
        date = date,
        driverId = DriverId.from(driverId),
        vehicleId = VehicleId.from(vehicleId),
        estimatedStartTime = estimatedStartTime,
        estimatedEndTime = estimatedEndTime,
        points = points
    )
}

data class RouteResponse(
    val id: String,
    val companyId: String,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: String,
    val vehicleId: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val childrenCount: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(result: CreateRouteResult) = RouteResponse(
            id = result.id.value,
            companyId = result.companyId.value,
            routeName = result.routeName,
            date = result.date,
            status = result.status,
            driverId = result.driverId.value,
            vehicleId = result.vehicleId.value,
            estimatedStartTime = result.estimatedStartTime,
            estimatedEndTime = result.estimatedEndTime,
            actualStartTime = null,
            actualEndTime = null,
            childrenCount = result.childrenCount,
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
    val childrenCount: Int
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
            childrenCount = item.childrenCount
        )
    }
}

data class RouteChildDetailResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val pickupOrder: Int,
    val pickupAddress: ScheduleAddressResponse,
    val dropoffAddress: ScheduleAddressResponse,
    @JsonFormat(pattern = "HH:mm")
    val estimatedPickupTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedDropoffTime: LocalTime,
    val actualPickupTime: Instant?,
    val actualDropoffTime: Instant?,
    val status: ChildInRouteStatus,
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
    val children: List<RouteChildDetailResponse>,
    val notes: List<RouteNoteResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(detail: RouteDetail) = RouteDetailResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
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
            children = detail.children.map { child ->
                RouteChildDetailResponse(
                    id = child.childId.value,
                    firstName = child.firstName,
                    lastName = child.lastName,
                    pickupOrder = child.pickupOrder,
                    pickupAddress = ScheduleAddressResponse.from(child.pickupAddress),
                    dropoffAddress = ScheduleAddressResponse.from(child.dropoffAddress),
                    estimatedPickupTime = child.estimatedPickupTime,
                    estimatedDropoffTime = child.estimatedDropoffTime,
                    actualPickupTime = child.actualPickupTime,
                    actualDropoffTime = child.actualDropoffTime,
                    status = child.status,
                    guardian = GuardianSimpleResponse(
                        firstName = child.guardianFirstName,
                        lastName = child.guardianLastName,
                        phone = child.guardianPhone
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

data class UpdateChildStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: ChildInRouteStatus,

    val actualPickupTime: Instant?,
    val actualDropoffTime: Instant?
)

data class UpdateChildStatusResponse(
    val childId: String,
    val status: ChildInRouteStatus,
    val actualPickupTime: Instant?,
    val actualDropoffTime: Instant?,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateChildStatusResult) = UpdateChildStatusResponse(
            childId = result.childId.value,
            status = result.status,
            actualPickupTime = result.actualPickupTime,
            actualDropoffTime = result.actualDropoffTime,
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