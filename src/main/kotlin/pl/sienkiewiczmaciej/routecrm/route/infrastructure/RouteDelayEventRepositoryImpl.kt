package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

@Repository
class RouteDelayEventRepositoryImpl(
    private val jpaRepository: RouteDelayEventJpaRepository
) : RouteDelayEventRepository {

    override suspend fun save(event: RouteDelayEvent): RouteDelayEvent =
        withContext(Dispatchers.IO) {
            try {
                val entity = RouteDelayEventEntity.fromDomain(event)
                jpaRepository.save(entity).toDomain()
            } catch (e: DataIntegrityViolationException) {
                event
            }
        }

    override suspend fun existsForStop(
        companyId: CompanyId,
        routeId: RouteId,
        stopId: RouteStopId,
        detectionType: DelayDetectionType
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndRouteIdAndStopIdAndDetectionType(
            companyId.value,
            routeId.value,
            stopId.value,
            detectionType
        )
    }

    override suspend fun findByRoute(
        companyId: CompanyId,
        routeId: RouteId
    ): List<RouteDelayEvent> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndRouteIdOrderByDetectedAtDesc(
            companyId.value,
            routeId.value
        ).map { it.toDomain() }
    }

    override suspend fun findByRouteGroupedByStop(
        companyId: CompanyId,
        routeId: RouteId
    ): Map<String, RouteDelayEvent> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndRouteIdOrderByDetectedAtDesc(
            companyId.value,
            routeId.value
        )
            .map { it.toDomain() }
            .groupBy { it.stopId.value }
            .mapValues { (_, events) -> events.maxBy { it.detectedAt } }
    }

    override suspend fun getStatistics(
        companyId: CompanyId,
        from: Instant,
        to: Instant
    ): DelayStatistics = withContext(Dispatchers.IO) {
        val projection = jpaRepository.getStatisticsNative(
            companyId.value,
            from,
            to
        )

        DelayStatistics(
            totalDelayedRoutes = projection.getTotalDelayedRoutes().toInt(),
            totalDelayedStops = projection.getTotalDelayedStops().toInt(),
            avgDelayMinutes = projection.getAvgDelayMinutes(),
            maxDelayMinutes = projection.getMaxDelayMinutes(),
            retrospectiveCount = projection.getRetrospectiveCount().toInt(),
            predictiveCount = projection.getPredictiveCount().toInt(),
            distribution = DelayDistribution(
                delays1to5 = projection.getDelays1to5().toInt(),
                delays6to10 = projection.getDelays6to10().toInt(),
                delays11to15 = projection.getDelays11to15().toInt(),
                delaysOver15 = projection.getDelaysOver15().toInt()
            )
        )
    }
}