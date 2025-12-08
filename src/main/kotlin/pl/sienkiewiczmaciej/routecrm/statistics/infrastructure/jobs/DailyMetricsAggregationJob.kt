package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure.jobs

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.application.DailyMetricsAggregationService
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository
import java.time.LocalDate

@Component
class DailyMetricsAggregationJob(
    private val aggregationService: DailyMetricsAggregationService,
    private val companyRepository: CompanyJpaRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 2 * * *")
    fun aggregateDailyMetrics() = runBlocking {
        val yesterday = LocalDate.now().minusDays(1)
        val startTime = System.currentTimeMillis()

        logger.info("Starting daily metrics aggregation job for date: $yesterday")

        val companies = companyRepository.findAll()
        var successCount = 0
        var failureCount = 0

        companies.forEach { company ->
            try {
                val result = aggregationService.aggregateMetricsForDate(
                    CompanyId(company.id),
                    yesterday
                )

                if (result.success) {
                    successCount++
                } else {
                    failureCount++
                    logger.error("Aggregation failed for company ${company.id}: ${result.error}")
                }
            } catch (e: Exception) {
                failureCount++
                logger.error("Unexpected error aggregating metrics for company ${company.id}", e)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info(
            "Daily metrics aggregation completed: $successCount succeeded, $failureCount failed, took ${duration}ms"
        )
    }
}