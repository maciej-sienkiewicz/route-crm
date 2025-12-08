package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure.jobs

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.statistics.infrastructure.*
import java.time.LocalDate

@Component
class MetricsCleanupJob(
    private val fleetMetricsJpaRepository: DailyFleetMetricsJpaRepository,
    private val capacityMetricsJpaRepository: DailyCapacityMetricsJpaRepository,
    private val workloadMetricsJpaRepository: DailyWorkloadMetricsJpaRepository,
    private val driverPerformanceJpaRepository: DriverDailyPerformanceJpaRepository,
    private val passengerMetricsJpaRepository: DailyPassengerMetricsJpaRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RETENTION_MONTHS = 18L
    }

    @Scheduled(cron = "0 0 3 1 * *")
    @Transactional
    fun cleanupOldMetrics() = runBlocking {
        val cutoffDate = LocalDate.now().minusMonths(RETENTION_MONTHS)
        logger.info("Starting metrics cleanup for data older than $cutoffDate")

        val startTime = System.currentTimeMillis()

        try {
            val fleetDeleted = fleetMetricsJpaRepository.findAll()
                .filter { it.metricDate.isBefore(cutoffDate) }
                .also { fleetMetricsJpaRepository.deleteAll(it) }
                .size

            val capacityDeleted = capacityMetricsJpaRepository.findAll()
                .filter { it.metricDate.isBefore(cutoffDate) }
                .also { capacityMetricsJpaRepository.deleteAll(it) }
                .size

            val workloadDeleted = workloadMetricsJpaRepository.findAll()
                .filter { it.metricDate.isBefore(cutoffDate) }
                .also { workloadMetricsJpaRepository.deleteAll(it) }
                .size

            val driverDeleted = driverPerformanceJpaRepository.findAll()
                .filter { it.metricDate.isBefore(cutoffDate) }
                .also { driverPerformanceJpaRepository.deleteAll(it) }
                .size

            val passengerDeleted = passengerMetricsJpaRepository.findAll()
                .filter { it.metricDate.isBefore(cutoffDate) }
                .also { passengerMetricsJpaRepository.deleteAll(it) }
                .size

            val totalDeleted = fleetDeleted + capacityDeleted + workloadDeleted + driverDeleted + passengerDeleted
            val duration = System.currentTimeMillis() - startTime

            logger.info(
                "Metrics cleanup completed: $totalDeleted records deleted " +
                        "(fleet: $fleetDeleted, capacity: $capacityDeleted, workload: $workloadDeleted, " +
                        "driver: $driverDeleted, passenger: $passengerDeleted) in ${duration}ms"
            )
        } catch (e: Exception) {
            logger.error("Metrics cleanup failed", e)
        }
    }
}