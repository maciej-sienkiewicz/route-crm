package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.route.domain.DelayDetectionType
import java.time.Instant

interface RouteDelayEventJpaRepository : JpaRepository<RouteDelayEventEntity, String> {

    fun existsByCompanyIdAndRouteIdAndStopIdAndDetectionType(
        companyId: String,
        routeId: String,
        stopId: String,
        detectionType: DelayDetectionType
    ): Boolean

    fun findByCompanyIdAndRouteIdOrderByDetectedAtDesc(
        companyId: String,
        routeId: String
    ): List<RouteDelayEventEntity>

    @Query("""
        SELECT 
            COUNT(DISTINCT e.routeId) as totalDelayedRoutes,
            COUNT(DISTINCT e.stopId) as totalDelayedStops,
            COALESCE(AVG(e.delayMinutes), 0) as avgDelayMinutes,
            COALESCE(MAX(e.delayMinutes), 0) as maxDelayMinutes,
            SUM(CASE WHEN e.detectionType = 'RETROSPECTIVE' THEN 1 ELSE 0 END) as retrospectiveCount,
            SUM(CASE WHEN e.detectionType = 'PREDICTIVE' THEN 1 ELSE 0 END) as predictiveCount,
            SUM(CASE WHEN e.delayMinutes BETWEEN 1 AND 5 THEN 1 ELSE 0 END) as delays1to5,
            SUM(CASE WHEN e.delayMinutes BETWEEN 6 AND 10 THEN 1 ELSE 0 END) as delays6to10,
            SUM(CASE WHEN e.delayMinutes BETWEEN 11 AND 15 THEN 1 ELSE 0 END) as delays11to15,
            SUM(CASE WHEN e.delayMinutes > 15 THEN 1 ELSE 0 END) as delaysOver15
        FROM RouteDelayEventEntity e
        WHERE e.companyId = :companyId
          AND e.detectedAt BETWEEN :from AND :to
    """)
    fun getStatisticsNative(
        @Param("companyId") companyId: String,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): StatisticsProjection

    interface StatisticsProjection {
        fun getTotalDelayedRoutes(): Long
        fun getTotalDelayedStops(): Long
        fun getAvgDelayMinutes(): Double
        fun getMaxDelayMinutes(): Int
        fun getRetrospectiveCount(): Long
        fun getPredictiveCount(): Long
        fun getDelays1to5(): Long
        fun getDelays6to10(): Long
        fun getDelays11to15(): Long
        fun getDelaysOver15(): Long
    }
}