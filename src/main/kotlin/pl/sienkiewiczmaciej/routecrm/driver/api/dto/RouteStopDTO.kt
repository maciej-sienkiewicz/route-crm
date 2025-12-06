package pl.sienkiewiczmaciej.routecrm.driver.api.dto

import pl.sienkiewiczmaciej.routecrm.route.domain.ExecutionStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import java.time.Instant
import java.time.LocalTime

data class RouteStopDTO(
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
) {
    companion object {
        fun fromDomain(stop: RouteStop, childFirstName: String, childLastName: String): DriverRouteStopDTO {
            return DriverRouteStopDTO(
                id = stop.id.value,
                order = stop.stopOrder,
                type = stop.stopType,
                child = DriverChildInfoDTO(
                    firstName = childFirstName,
                    lastName = childLastName
                ),
                address = DriverAddressDTO(
                    label = stop.address.label,
                    street = stop.address.address.street,
                    houseNumber = stop.address.address.houseNumber,
                    apartmentNumber = stop.address.address.apartmentNumber,
                    city = stop.address.address.city,
                    postalCode = stop.address.address.postalCode,
                    latitude = stop.address.latitude,
                    longitude = stop.address.longitude
                ),
                estimatedTime = stop.estimatedTime,
                actualTime = stop.actualTime,
                status = stop.executionStatus,
                isCancelled = stop.isCancelled,
                notes = stop.executionNotes
            )
        }
    }
}

data class ChildInfoDTO(
    val firstName: String,
    val lastName: String
)

data class AddressDTO(
    val label: String?,
    val street: String,
    val houseNumber: String,
    val apartmentNumber: String?,
    val city: String,
    val postalCode: String,
    val latitude: Double?,
    val longitude: Double?
)