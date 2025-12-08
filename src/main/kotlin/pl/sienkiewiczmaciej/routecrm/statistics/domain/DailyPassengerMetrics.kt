package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class DailyPassengerMetrics(
    val companyId: CompanyId,
    val metricDate: LocalDate,
    val avgTripDurationMinutes: Double,
    val p50TripDurationMinutes: Int,
    val p90TripDurationMinutes: Int,
    val p99TripDurationMinutes: Int,
    val totalTrips: Int,
    val tripsOverP90Count: Int
)