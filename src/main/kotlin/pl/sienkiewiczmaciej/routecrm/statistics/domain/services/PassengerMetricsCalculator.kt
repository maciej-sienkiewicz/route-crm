package pl.sienkiewiczmaciej.routecrm.statistics.domain.services

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyPassengerMetrics
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PassengerMetricsCalculator(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository
) {
    suspend fun calculate(companyId: CompanyId, date: LocalDate): DailyPassengerMetrics {
        val routes = routeJpaRepository.findByFilters(
            companyId.value,
            date,
            null,
            null,
            org.springframework.data.domain.Pageable.unpaged()
        ).content.filter {
            it.status in setOf(RouteStatus.COMPLETED)
        }

        val tripDurations = routes.flatMap { route ->
            val stops = routeStopJpaRepository.findByCompanyIdAndRouteIdOrderByStopOrder(
                companyId.value,
                route.id
            ).filter { !it.isCancelled && it.actualTime != null }

            val stopsByChild = stops.groupBy { it.childId }

            stopsByChild.mapNotNull { (childId, childStops) ->
                val pickup = childStops.find { it.stopType == StopType.PICKUP }
                val dropoff = childStops.find { it.stopType == StopType.DROPOFF }

                if (pickup?.actualTime != null && dropoff?.actualTime != null) {
                    val duration = ChronoUnit.MINUTES.between(pickup.actualTime, dropoff.actualTime)
                    duration.toInt()
                } else null
            }
        }

        if (tripDurations.isEmpty()) {
            return DailyPassengerMetrics(
                companyId = companyId,
                metricDate = date,
                avgTripDurationMinutes = 0.0,
                p50TripDurationMinutes = 0,
                p90TripDurationMinutes = 0,
                p99TripDurationMinutes = 0,
                totalTrips = 0,
                tripsOverP90Count = 0
            )
        }

        val sorted = tripDurations.sorted()
        val avg = tripDurations.average()
        val p50 = percentile(sorted, 50)
        val p90 = percentile(sorted, 90)
        val p99 = percentile(sorted, 99)
        val overP90 = tripDurations.count { it > p90 }

        return DailyPassengerMetrics(
            companyId = companyId,
            metricDate = date,
            avgTripDurationMinutes = avg,
            p50TripDurationMinutes = p50,
            p90TripDurationMinutes = p90,
            p99TripDurationMinutes = p99,
            totalTrips = tripDurations.size,
            tripsOverP90Count = overP90
        )
    }

    private fun percentile(sorted: List<Int>, percentile: Int): Int {
        if (sorted.isEmpty()) return 0
        val index = (percentile / 100.0 * sorted.size).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }
}