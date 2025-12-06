package pl.sienkiewiczmaciej.routecrm.driver.api.dto

import jakarta.validation.constraints.NotBlank
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Vehicle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class DriverRouteDTO(
    val id: String,
    val name: String,
    val date: LocalDate,
    val status: RouteStatus,
    val vehicleInfo: VehicleInfoDTO,
    val estimatedStart: LocalTime,
    val estimatedEnd: LocalTime,
    val actualStart: Instant?,
    val stopsTotal: Int,
    val stopsCompleted: Int
)

data class VehicleInfoDTO(
    val registration: String,
    val make: String,
    val model: String
)

data class RouteDetailResponse(
    val route: DriverRouteDTO,
    val stops: List<DriverRouteStopDTO>
)

data class DriverRouteStopDTO(
    val id: String,
    val order: Int,
    val type: StopType,
    val child: DriverChildInfoDTO,
    val address: DriverAddressDTO,
    val estimatedTime: LocalTime,
    val actualTime: Instant?,
    val status: ExecutionStatus?,
    val isCancelled: Boolean,
    val notes: String?
)

data class DriverChildInfoDTO(
    val firstName: String,
    val lastName: String
)

data class DriverAddressDTO(
    val label: String?,
    val street: String,
    val houseNumber: String,
    val apartmentNumber: String?,
    val city: String,
    val postalCode: String,
    val latitude: Double?,
    val longitude: Double?
)

data class ExecuteStopRequest(
    @field:NotBlank
    val action: String,
    val reason: String? = null,
    val notes: String? = null,
    val timestamp: Instant? = null
)

enum class StopAction {
    COMPLETE,
    SKIP
}

enum class SkipReason {
    CHILD_NOT_PRESENT,
    PARENT_CANCELLED,
    SAFETY_CONCERN,
    TIME_CONSTRAINT,
    OTHER
}

data class ExecuteStopResponse(
    val stop: DriverRouteStopDTO,
    val route: RouteStatusDTO
)

data class RouteStatusDTO(
    val id: String,
    val status: RouteStatus,
    val stopsCompleted: Int,
    val stopsTotal: Int
)

fun Route.toDriverDTO(vehicle: Vehicle, stopsTotal: Int, stopsCompleted: Int) = DriverRouteDTO(
    id = id.value,
    name = routeName,
    date = date,
    status = status,
    vehicleInfo = VehicleInfoDTO(
        registration = vehicle.registrationNumber,
        make = vehicle.make,
        model = vehicle.model
    ),
    estimatedStart = estimatedStartTime,
    estimatedEnd = estimatedEndTime,
    actualStart = actualStartTime,
    stopsTotal = stopsTotal,
    stopsCompleted = stopsCompleted
)

fun RouteStop.toDTO(child: Child) = DriverRouteStopDTO(
    id = id.value,
    order = stopOrder,
    type = stopType,
    child = DriverChildInfoDTO(
        firstName = child.firstName,
        lastName = child.lastName
    ),
    address = DriverAddressDTO(
        label = address.label,
        street = address.address.street,
        houseNumber = address.address.houseNumber,
        apartmentNumber = address.address.apartmentNumber,
        city = address.address.city,
        postalCode = address.address.postalCode,
        latitude = address.latitude,
        longitude = address.longitude
    ),
    estimatedTime = estimatedTime,
    actualTime = actualTime,
    status = executionStatus,
    isCancelled = isCancelled,
    notes = executionNotes
)