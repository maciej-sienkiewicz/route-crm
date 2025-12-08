package pl.sienkiewiczmaciej.routecrm.statistics.domain.services

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyCapacityMetrics
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.LocalDate
import kotlin.math.max

@Service
class CapacityMetricsCalculator(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val childJpaRepository: ChildJpaRepository
) {
    suspend fun calculate(companyId: CompanyId, date: LocalDate): DailyCapacityMetrics {
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
            return DailyCapacityMetrics(
                companyId = companyId,
                metricDate = date,
                avgSaturationPercent = 0.0,
                minSaturationPercent = 0.0,
                maxSaturationPercent = 0.0,
                totalRoutes = 0,
                routesLowSaturation = 0
            )
        }

        val saturations = routes.mapNotNull { route ->
            calculateRouteSaturation(companyId, route.id, route.vehicleId)
        }

        val avgSaturation = saturations.average()
        val minSaturation = saturations.minOrNull() ?: 0.0
        val maxSaturation = saturations.maxOrNull() ?: 0.0
        val lowSaturationCount = saturations.count { it < 70.0 }

        return DailyCapacityMetrics(
            companyId = companyId,
            metricDate = date,
            avgSaturationPercent = avgSaturation,
            minSaturationPercent = minSaturation,
            maxSaturationPercent = maxSaturation,
            totalRoutes = routes.size,
            routesLowSaturation = lowSaturationCount
        )
    }

    private fun calculateRouteSaturation(companyId: CompanyId, routeId: String, vehicleId: String): Double? {
        val stops = routeStopJpaRepository.findByCompanyIdAndRouteIdOrderByStopOrder(
            companyId.value,
            routeId
        ).filter { !it.isCancelled }

        if (stops.isEmpty()) return null

        val vehicle = vehicleJpaRepository.findByIdAndCompanyId(vehicleId, companyId.value) ?: return null

        var currentOccupancy = 0
        var maxOccupancy = 0
        var totalOccupancy = 0
        var segmentCount = 0

        stops.forEach { stop ->
            when (stop.stopType) {
                StopType.PICKUP -> currentOccupancy++
                StopType.DROPOFF -> currentOccupancy--
            }
            maxOccupancy = max(maxOccupancy, currentOccupancy)
            totalOccupancy += currentOccupancy
            segmentCount++
        }

        val avgOccupancy = if (segmentCount > 0) totalOccupancy.toDouble() / segmentCount else 0.0
        val capacity = vehicle.capacityTotalSeats

        return if (capacity > 0) (avgOccupancy / capacity) * 100.0 else 0.0
    }
}