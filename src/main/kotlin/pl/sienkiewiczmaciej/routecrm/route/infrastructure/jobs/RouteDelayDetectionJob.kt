package pl.sienkiewiczmaciej.routecrm.route.infrastructure.jobs

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class RouteDelayDetectionJob(
    private val companyRepository: CompanyJpaRepository,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val delayEventRepository: RouteDelayEventRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60000)
    fun detectPredictiveDelays() = runBlocking {
        val startTime = System.currentTimeMillis()
        var processedRoutes = 0
        var detectedDelays = 0

        try {
            val companies = companyRepository.findAll()

            companies.forEach { company ->
                try {
                    val result = detectDelaysForCompany(CompanyId(company.id))
                    processedRoutes += result.processedRoutes
                    detectedDelays += result.detectedDelays
                } catch (e: Exception) {
                    logger.error("Failed to detect delays for company ${company.id}", e)
                }
            }
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logger.info(
                "Delay detection completed: $processedRoutes routes processed, " +
                        "$detectedDelays delays detected in ${duration}ms"
            )
        }
    }

    @Transactional
    suspend fun detectDelaysForCompany(companyId: CompanyId): DelayDetectionResult = coroutineScope {
        val activeRoutes = routeRepository.findAll(
            companyId = companyId,
            date = LocalDate.now(),
            status = RouteStatus.IN_PROGRESS,
            driverId = null,
            pageable = org.springframework.data.domain.Pageable.unpaged()
        ).content

        if (activeRoutes.isEmpty()) {
            return@coroutineScope DelayDetectionResult(0, 0)
        }

        val delayChecks = activeRoutes.map { route ->
            async {
                detectDelayForRoute(companyId, route)
            }
        }.awaitAll()

        DelayDetectionResult(
            processedRoutes = activeRoutes.size,
            detectedDelays = delayChecks.count { it }
        )
    }

    private suspend fun detectDelayForRoute(companyId: CompanyId, route: Route): Boolean {
        val nextStop = stopRepository.findNextUnexecutedStop(companyId, route.id)
            ?: return false

        val estimatedDateTime = nextStop.estimatedTime
            .atDate(route.date)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val now = Instant.now()
        val overdue = Duration.between(estimatedDateTime, now)

        if (overdue.toMinutes() >= DELAY_THRESHOLD_MINUTES) {
            val exists = delayEventRepository.existsForStop(
                companyId = companyId,
                routeId = route.id,
                stopId = nextStop.id,
                detectionType = DelayDetectionType.PREDICTIVE
            )

            if (!exists) {
                delayEventRepository.save(
                    RouteDelayEvent.predictive(
                        companyId = companyId,
                        routeId = route.id,
                        stopId = nextStop.id,
                        delayMinutes = overdue.toMinutes().toInt(),
                        detectedAt = now
                    )
                )

                logger.info(
                    "Predictive delay detected: Route ${route.id.value}, " +
                            "Stop ${nextStop.id.value}, Overdue: ${overdue.toMinutes()} minutes"
                )

                return true
            }
        }

        return false
    }

    companion object {
        private const val DELAY_THRESHOLD_MINUTES = 3L
    }
}

data class DelayDetectionResult(
    val processedRoutes: Int,
    val detectedDelays: Int
)