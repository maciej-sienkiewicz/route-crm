package pl.sienkiewiczmaciej.routecrm.statistics.application

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.api.dto.DriverPerformanceSummaryDto
import pl.sienkiewiczmaciej.routecrm.statistics.api.mappers.StatisticsMappers.toDto
import pl.sienkiewiczmaciej.routecrm.statistics.domain.*
import java.time.LocalDate

@Service
class StatisticsQueryService(
    private val fleetMetricsRepository: DailyFleetMetricsRepository,
    private val capacityMetricsRepository: DailyCapacityMetricsRepository,
    private val workloadMetricsRepository: DailyWorkloadMetricsRepository,
    private val driverPerformanceRepository: DriverDailyPerformanceRepository,
    private val passengerMetricsRepository: DailyPassengerMetricsRepository,
    private val driverJpaRepository: DriverJpaRepository
) {
    suspend fun getFleetMetrics(companyId: CompanyId, startDate: LocalDate, endDate: LocalDate) =
        fleetMetricsRepository.findByCompanyAndDateRange(companyId, startDate, endDate)
            .map { it.toDto() }

    suspend fun getCapacityMetrics(companyId: CompanyId, startDate: LocalDate, endDate: LocalDate) =
        capacityMetricsRepository.findByCompanyAndDateRange(companyId, startDate, endDate)
            .map { it.toDto() }

    suspend fun getWorkloadMetrics(companyId: CompanyId, startDate: LocalDate, endDate: LocalDate) =
        workloadMetricsRepository.findByCompanyAndDateRange(companyId, startDate, endDate)
            .map { it.toDto() }

    suspend fun getPassengerMetrics(companyId: CompanyId, startDate: LocalDate, endDate: LocalDate) =
        passengerMetricsRepository.findByCompanyAndDateRange(companyId, startDate, endDate)
            .map { it.toDto() }

    suspend fun getDriverPerformance(
        companyId: CompanyId,
        driverId: DriverId,
        startDate: LocalDate,
        endDate: LocalDate
    ): DriverPerformanceSummaryDto {
        val performances = driverPerformanceRepository.findByDriverAndDateRange(
            companyId,
            driverId,
            startDate,
            endDate
        )

        val driver = driverJpaRepository.findByIdAndCompanyId(driverId.value, companyId.value)
        val driverName = driver?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"

        val totalStops = performances.sumOf { it.totalStops }
        val totalOntime = performances.sumOf { it.ontimeStops }
        val totalLate = performances.sumOf { it.lateStops }
        val avgOtd = if (totalStops > 0) (totalOntime.toDouble() / totalStops) * 100 else 0.0
        val avgDelay = if (performances.isNotEmpty()) performances.map { it.avgDelayMinutes }.average().toInt() else 0
        val maxDelay = performances.maxOfOrNull { it.maxDelayMinutes } ?: 0

        return DriverPerformanceSummaryDto(
            driverId = driverId.value,
            driverName = driverName,
            period = "$startDate to $endDate",
            totalStops = totalStops,
            ontimeStops = totalOntime,
            lateStops = totalLate,
            otdPercentage = avgOtd,
            avgDelayMinutes = avgDelay,
            maxDelayMinutes = maxDelay,
            dailyBreakdown = performances.map { it.toDto() }
        )
    }

    suspend fun getAllDriversPerformance(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ) = driverPerformanceRepository.findByCompanyAndDateRange(companyId, startDate, endDate)
        .groupBy { it.driverId }
        .map { (driverId, performances) ->
            getDriverPerformance(companyId, driverId, startDate, endDate)
        }
        .sortedByDescending { it.otdPercentage }
}