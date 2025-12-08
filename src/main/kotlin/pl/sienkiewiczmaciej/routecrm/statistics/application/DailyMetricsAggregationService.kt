package pl.sienkiewiczmaciej.routecrm.statistics.application

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.*
import pl.sienkiewiczmaciej.routecrm.statistics.domain.services.*
import java.time.LocalDate

@Service
class DailyMetricsAggregationService(
    private val fleetMetricsCalculator: FleetMetricsCalculator,
    private val capacityMetricsCalculator: CapacityMetricsCalculator,
    private val workloadMetricsCalculator: WorkloadMetricsCalculator,
    private val driverPerformanceCalculator: DriverPerformanceCalculator,
    private val passengerMetricsCalculator: PassengerMetricsCalculator,
    private val fleetMetricsRepository: DailyFleetMetricsRepository,
    private val capacityMetricsRepository: DailyCapacityMetricsRepository,
    private val workloadMetricsRepository: DailyWorkloadMetricsRepository,
    private val driverPerformanceRepository: DriverDailyPerformanceRepository,
    private val passengerMetricsRepository: DailyPassengerMetricsRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    suspend fun aggregateMetricsForDate(companyId: CompanyId, date: LocalDate): AggregationResult = coroutineScope {
        logger.info("Starting metrics aggregation for company ${companyId.value}, date $date")

        try {
            deleteExistingMetrics(companyId, date)

            val fleetDeferred = async { calculateAndSaveFleetMetrics(companyId, date) }
            val capacityDeferred = async { calculateAndSaveCapacityMetrics(companyId, date) }
            val workloadDeferred = async { calculateAndSaveWorkloadMetrics(companyId, date) }
            val driverDeferred = async { calculateAndSaveDriverPerformance(companyId, date) }
            val passengerDeferred = async { calculateAndSavePassengerMetrics(companyId, date) }

            fleetDeferred.await()
            capacityDeferred.await()
            workloadDeferred.await()
            val driverCount = driverDeferred.await()
            passengerDeferred.await()

            logger.info("Successfully aggregated metrics for company ${companyId.value}, date $date")
            AggregationResult(success = true, date = date, driverMetricsCount = driverCount)
        } catch (e: Exception) {
            logger.error("Failed to aggregate metrics for company ${companyId.value}, date $date", e)
            AggregationResult(success = false, date = date, error = e.message)
        }
    }

    private suspend fun deleteExistingMetrics(companyId: CompanyId, date: LocalDate) {
        fleetMetricsRepository.deleteByCompanyAndDate(companyId, date)
        capacityMetricsRepository.deleteByCompanyAndDate(companyId, date)
        workloadMetricsRepository.deleteByCompanyAndDate(companyId, date)
        driverPerformanceRepository.deleteByCompanyAndDate(companyId, date)
        passengerMetricsRepository.deleteByCompanyAndDate(companyId, date)
    }

    private suspend fun calculateAndSaveFleetMetrics(companyId: CompanyId, date: LocalDate) {
        val metrics = fleetMetricsCalculator.calculate(companyId, date)
        fleetMetricsRepository.save(metrics)
    }

    private suspend fun calculateAndSaveCapacityMetrics(companyId: CompanyId, date: LocalDate) {
        val metrics = capacityMetricsCalculator.calculate(companyId, date)
        capacityMetricsRepository.save(metrics)
    }

    private suspend fun calculateAndSaveWorkloadMetrics(companyId: CompanyId, date: LocalDate) {
        val metrics = workloadMetricsCalculator.calculate(companyId, date)
        workloadMetricsRepository.save(metrics)
    }

    private suspend fun calculateAndSaveDriverPerformance(companyId: CompanyId, date: LocalDate): Int {
        val performances = driverPerformanceCalculator.calculate(companyId, date)
        performances.forEach { driverPerformanceRepository.save(it) }
        return performances.size
    }

    private suspend fun calculateAndSavePassengerMetrics(companyId: CompanyId, date: LocalDate) {
        val metrics = passengerMetricsCalculator.calculate(companyId, date)
        passengerMetricsRepository.save(metrics)
    }
}

data class AggregationResult(
    val success: Boolean,
    val date: LocalDate,
    val driverMetricsCount: Int = 0,
    val error: String? = null
)