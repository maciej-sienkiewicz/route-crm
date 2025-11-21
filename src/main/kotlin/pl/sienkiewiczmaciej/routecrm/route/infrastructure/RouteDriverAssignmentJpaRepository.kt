// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteDriverAssignmentJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RouteDriverAssignmentJpaRepository : JpaRepository<RouteDriverAssignmentEntity, String> {

    fun findByCompanyIdAndRouteIdOrderByReassignedAtAsc(
        companyId: String,
        routeId: String
    ): List<RouteDriverAssignmentEntity>

    @Query("""
        SELECT a FROM RouteDriverAssignmentEntity a
        WHERE a.companyId = :companyId
        AND a.routeId = :routeId
        ORDER BY a.reassignedAt ASC
        LIMIT 1
    """)
    fun findFirstByRoute(
        @Param("companyId") companyId: String,
        @Param("routeId") routeId: String
    ): RouteDriverAssignmentEntity?

    @Query("""
        SELECT a FROM RouteDriverAssignmentEntity a
        WHERE a.companyId = :companyId
        AND a.reassignedAt >= :from
        AND a.reassignedAt <= :to
        ORDER BY a.reassignedAt DESC
    """)
    fun findInDateRange(
        @Param("companyId") companyId: String,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        pageable: Pageable
    ): Page<RouteDriverAssignmentEntity>

    @Query("""
        SELECT a FROM RouteDriverAssignmentEntity a
        WHERE a.companyId = :companyId
        AND a.newDriverId = :driverId
        AND a.reassignedAt >= :from
        AND a.reassignedAt <= :to
        ORDER BY a.reassignedAt DESC
    """)
    fun findByDriver(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<RouteDriverAssignmentEntity>

    fun countByCompanyIdAndRouteId(
        companyId: String,
        routeId: String
    ): Int
}