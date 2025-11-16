// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/DriverRoutesDTOs.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes

import com.fasterxml.jackson.annotation.JsonFormat
import pl.sienkiewiczmaciej.routecrm.driver.routes.history.DriverRouteHistoryItem
import pl.sienkiewiczmaciej.routecrm.driver.routes.upcoming.DriverUpcomingRouteItem
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class DriverRouteHistoryResponse(
    val id: String,
    val routeName: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val date: LocalDate,
    val status: RouteStatus,
    val vehicleId: String,
    val vehicleRegistrationNumber: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val stopsCount: Int,
    val completedStopsCount: Int,
    val childrenCount: Int,
    val wasPunctual: Boolean,
    val delayMinutes: Int?
) {
    companion object {
        fun from(item: DriverRouteHistoryItem) = DriverRouteHistoryResponse(
            id = item.id.value,
            routeName = item.routeName,
            date = item.date,
            status = item.status,
            vehicleId = item.vehicleId.value,
            vehicleRegistrationNumber = item.vehicleRegistrationNumber,
            estimatedStartTime = item.estimatedStartTime,
            estimatedEndTime = item.estimatedEndTime,
            actualStartTime = item.actualStartTime,
            actualEndTime = item.actualEndTime,
            stopsCount = item.stopsCount,
            completedStopsCount = item.completedStopsCount,
            childrenCount = item.childrenCount,
            wasPunctual = item.wasPunctual,
            delayMinutes = item.delayMinutes
        )
    }
}

data class DriverUpcomingRouteResponse(
    val id: String,
    val routeName: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val date: LocalDate,
    val status: RouteStatus,
    val vehicleId: String,
    val vehicleRegistrationNumber: String,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime,
    val stopsCount: Int,
    val childrenCount: Int,
    val firstStopAddress: String,
    val lastStopAddress: String
) {
    companion object {
        fun from(item: DriverUpcomingRouteItem) = DriverUpcomingRouteResponse(
            id = item.id.value,
            routeName = item.routeName,
            date = item.date,
            status = item.status,
            vehicleId = item.vehicleId.value,
            vehicleRegistrationNumber = item.vehicleRegistrationNumber,
            estimatedStartTime = item.estimatedStartTime,
            estimatedEndTime = item.estimatedEndTime,
            stopsCount = item.stopsCount,
            childrenCount = item.childrenCount,
            firstStopAddress = item.firstStopAddress,
            lastStopAddress = item.lastStopAddress
        )
    }
}