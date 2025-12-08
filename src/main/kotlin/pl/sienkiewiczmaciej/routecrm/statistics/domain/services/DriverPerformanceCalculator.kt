package pl.sienkiewiczmaciej.routecrm.statistics.domain.services

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.route.domain.ExecutionStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DriverDailyPerformance
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class DriverPerformanceCalculator(
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository
) {
    companion object {
        private const val TOLERANCE_MINUTES = 5L
    }

    suspend fun calculate(companyId: CompanyId, date: LocalDate): List<DriverDailyPerformance> {
        val routes = routeJpaRepository.findByFilters(
            companyId.value,
            date,
            null,
            null,
            org.springframework.data.domain.Pageable.unpaged()
        ).content.filter {
            it.status in setOf(RouteStatus.COMPLETED) && it.driverId != null
        }

        val driverPerformances = routes.groupBy { it.driverId }
            .mapNotNull { (driverId, driverRoutes) ->
                if (driverId == null) return@mapNotNull null
                calculateDriverPerformance(companyId, driverId, date, driverRoutes.map { it.id })
            }

        return driverPerformances
    }

    private fun calculateDriverPerformance(
        companyId: CompanyId,
        driverId: String,
        date: LocalDate,
        routeIds: List<String>
    ): DriverDailyPerformance {
        val allStops = routeIds.flatMap { routeId ->
            routeStopJpaRepository.findByCompanyIdAndRouteIdOrderByStopOrder(
                companyId.value,
                routeId
            ).filter { !it.isCancelled && it.executionStatus == ExecutionStatus.COMPLETED }
        }

        if (allStops.isEmpty()) {
            return DriverDailyPerformance(
                companyId = companyId,
                driverId = pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId(driverId),
                metricDate = date,
                totalStops = 0,
                ontimeStops = 0,
                lateStops = 0,
                otdPercentage = 0.0,
                avgDelayMinutes = 0,
                maxDelayMinutes = 0
            )
        }

        val delays = allStops.mapNotNull { stop ->
            val actualTime = stop.actualTime ?: return@mapNotNull null
            val estimatedDateTime = stop.estimatedTime.atDate(date).atZone(ZoneId.systemDefault()).toInstant()
            val delayMinutes = ChronoUnit.MINUTES.between(estimatedDateTime, actualTime)
            delayMinutes.toInt()
        }

        val ontimeStops = delays.count { it <= TOLERANCE_MINUTES }
        val lateStops = delays.count { it > TOLERANCE_MINUTES }
        val otdPercentage = if (allStops.isNotEmpty()) (ontimeStops.toDouble() / allStops.size) * 100 else 0.0
        val avgDelay = if (delays.isNotEmpty()) delays.filter { it > 0 }.average().toInt() else 0
        val maxDelay = delays.maxOrNull() ?: 0

        return DriverDailyPerformance(
            companyId = companyId,
            driverId = pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId(driverId),
            metricDate = date,
            totalStops = allStops.size,
            ontimeStops = ontimeStops,
            lateStops = lateStops,
            otdPercentage = otdPercentage,
            avgDelayMinutes = avgDelay,
            maxDelayMinutes = maxDelay
        )
    }
}