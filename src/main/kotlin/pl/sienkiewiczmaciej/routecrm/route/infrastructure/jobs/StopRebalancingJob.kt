package pl.sienkiewiczmaciej.routecrm.route.infrastructure.jobs

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.services.GapBasedStopOrderCalculator
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository

@Component
class StopRebalancingJob(
    private val companyRepository: CompanyJpaRepository,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val calculator: GapBasedStopOrderCalculator
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun rebalanceCompressedRoutes() = runBlocking {
        logger.info("Starting nightly stop rebalancing job")

        val companies = companyRepository.findAll()
        var totalRebalanced = 0

        companies.forEach { company ->
            try {
                val routes = routeRepository.findAll(
                    companyId = pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId(company.id),
                    date = null,
                    status = RouteStatus.PLANNED,
                    driverId = null,
                    pageable = org.springframework.data.domain.Pageable.unpaged()
                ).content

                routes.forEach { route ->
                    val stops = stopRepository.findByRoute(
                        companyId = pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId(company.id),
                        routeId = route.id,
                        includeCancelled = false
                    )

                    if (calculator.needsRebalancing(stops)) {
                        val rebalanced = calculator.rebalance(stops)
                        stopRepository.saveAll(rebalanced)
                        totalRebalanced++
                        logger.info("Rebalanced route ${route.id.value} for company ${company.id}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to rebalance routes for company ${company.id}", e)
            }
        }

        logger.info("Nightly rebalancing complete: $totalRebalanced routes rebalanced")
    }
}