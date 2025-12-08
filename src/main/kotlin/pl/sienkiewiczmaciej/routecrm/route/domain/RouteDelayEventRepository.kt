package pl.sienkiewiczmaciej.routecrm.route.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

interface RouteDelayEventRepository {
    suspend fun save(event: RouteDelayEvent): RouteDelayEvent

    suspend fun existsForStop(
        companyId: CompanyId,
        routeId: RouteId,
        stopId: RouteStopId,
        detectionType: DelayDetectionType
    ): Boolean

    suspend fun findByRoute(
        companyId: CompanyId,
        routeId: RouteId
    ): List<RouteDelayEvent>

    suspend fun findByRouteGroupedByStop(
        companyId: CompanyId,
        routeId: RouteId
    ): Map<String, RouteDelayEvent>

    suspend fun getStatistics(
        companyId: CompanyId,
        from: Instant,
        to: Instant
    ): DelayStatistics
}

data class DelayStatistics(
    val totalDelayedRoutes: Int,
    val totalDelayedStops: Int,
    val avgDelayMinutes: Double,
    val maxDelayMinutes: Int,
    val retrospectiveCount: Int,
    val predictiveCount: Int,
    val distribution: DelayDistribution
)

data class DelayDistribution(
    val delays1to5: Int,
    val delays6to10: Int,
    val delays11to15: Int,
    val delaysOver15: Int
)