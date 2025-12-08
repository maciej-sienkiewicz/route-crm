package pl.sienkiewiczmaciej.routecrm.statistics.api.dto

import java.time.LocalDate

data class PassengerMetricsDto(
    val metricDate: LocalDate,
    val avgTripDurationMinutes: Double,
    val p50TripDurationMinutes: Int,
    val p90TripDurationMinutes: Int,
    val p99TripDurationMinutes: Int,
    val totalTrips: Int,
    val tripsOverP90Count: Int
)