// routeseries/infrastructure/schedulers/RouteSeriesMaterializationScheduler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.infrastructure.schedulers

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.RouteSeriesMaterializationService
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository
import java.time.LocalDate

@Component
class RouteSeriesMaterializationScheduler(
    private val materializationService: RouteSeriesMaterializationService,
    private val companyRepository: CompanyJpaRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 2 * * *")
    fun dailyMaterialization() = runBlocking {
        logger.info("Starting daily route series materialization")

        val today = LocalDate.now()
        val dateRange = today..today.plusDays(14)

        try {
            val companies = companyRepository.findAll()

            companies.forEach { company ->
                try {
                    val result = materializationService.materializeForDateRange(
                        companyId = CompanyId(company.id),
                        dateRange = dateRange,
                        forceRegenerate = false
                    )

                    logger.info(
                        "Materialization complete for company ${company.id}: " +
                                "created=${result.routesCreated}, " +
                                "skipped=${result.routesSkipped}, " +
                                "updated=${result.routesUpdated}"
                    )
                } catch (e: Exception) {
                    logger.error("Materialization failed for company ${company.id}", e)
                }
            }

        } catch (e: Exception) {
            logger.error("Daily materialization process failed", e)
        }
    }
}