// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/infrastructure/RouteSeriesMaterializationScheduler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.infrastructure

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.RouteSeriesMaterializationService
import java.time.LocalDate

@Component
class RouteSeriesMaterializationScheduler(
    private val materializationService: RouteSeriesMaterializationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Daily materialization at 2 AM
     * Generates routes for next 14 days
     */
    @Scheduled(cron = "0 0 2 * * *")
    fun dailyMaterialization() = runBlocking {
        logger.info("Starting daily route series materialization")

        val today = LocalDate.now()
        val dateRange = today..today.plusDays(14)

        try {
            val result = materializationService.materializeForDateRange(
                dateRange = dateRange,
                forceRegenerate = false
            )

            logger.info(
                "Daily materialization complete: " +
                        "created=${result.routesCreated}, " +
                        "skipped=${result.routesSkipped}, " +
                        "updated=${result.routesUpdated}"
            )

        } catch (e: Exception) {
            logger.error("Daily materialization failed", e)
        }
    }
}