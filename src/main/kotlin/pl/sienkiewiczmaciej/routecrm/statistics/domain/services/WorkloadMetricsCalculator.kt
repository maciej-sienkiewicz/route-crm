package pl.sienkiewiczmaciej.routecrm.statistics.domain.services

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyWorkloadMetrics
import java.time.LocalDate
import kotlin.math.sqrt

@Service
class WorkloadMetricsCalculator(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository
) {
    suspend fun calculate(companyId: CompanyId, date: LocalDate): DailyWorkloadMetrics {
        val routes = routeJpaRepository.findByFilters(
            companyId.value,
            date,
            null,
            null,
            org.springframework.data.domain.Pageable.unpaged()
        ).content.filter {
            it.status in setOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS, RouteStatus.COMPLETED)
        }

        if (routes.isEmpty()) {
            return DailyWorkloadMetrics(
                companyId = companyId,
                metricDate = date,
                avgStopsPerVehicle = 0.0,
                minStopsPerVehicle = 0,
                maxStopsPerVehicle = 0,
                stdDevStops = 0.0,
                avgStopsPerDriver = 0.0,
                minStopsPerDriver = 0,
                maxStopsPerDriver = 0
            )
        }

        val stopsPerVehicle = routes.map { route ->
            val stops = routeStopJpaRepository.findByCompanyIdAndRouteIdOrderByStopOrder(
                companyId.value,
                route.id
            ).filter { !it.isCancelled }
            route.vehicleId to stops.size
        }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }

        val stopsPerDriver = routes.mapNotNull { route ->
            route.driverId?.let { driverId ->
                val stops = routeStopJpaRepository.findByCompanyIdAndRouteIdOrderByStopOrder(
                    companyId.value,
                    route.id
                ).filter { !it.isCancelled }
                driverId to stops.size
            }
        }.groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }

        val vehicleStopCounts = stopsPerVehicle.values.toList()
        val driverStopCounts = stopsPerDriver.values.toList()

        val avgStopsPerVehicle = vehicleStopCounts.average()
        val minStopsPerVehicle = vehicleStopCounts.minOrNull() ?: 0
        val maxStopsPerVehicle = vehicleStopCounts.maxOrNull() ?: 0
        val stdDev = calculateStdDev(vehicleStopCounts)

        val avgStopsPerDriver = if (driverStopCounts.isNotEmpty()) driverStopCounts.average() else 0.0
        val minStopsPerDriver = driverStopCounts.minOrNull() ?: 0
        val maxStopsPerDriver = driverStopCounts.maxOrNull() ?: 0

        return DailyWorkloadMetrics(
            companyId = companyId,
            metricDate = date,
            avgStopsPerVehicle = avgStopsPerVehicle,
            minStopsPerVehicle = minStopsPerVehicle,
            maxStopsPerVehicle = maxStopsPerVehicle,
            stdDevStops = stdDev,
            avgStopsPerDriver = avgStopsPerDriver,
            minStopsPerDriver = minStopsPerDriver,
            maxStopsPerDriver = maxStopsPerDriver
        )
    }

    private fun calculateStdDev(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
}