// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteJpaRepositoryExtension.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import java.time.LocalDate

interface RouteJpaRepositoryExtension {
    @Query("""
        SELECT COUNT(DISTINCT r.id)
        FROM RouteEntity r
        JOIN RouteStopEntity rs ON rs.routeId = r.id AND rs.companyId = r.companyId
        WHERE r.companyId = :companyId
          AND rs.childId IN :childIds
          AND r.date >= :fromDate
          AND r.status IN :statuses
          AND rs.isCancelled = false
    """)
    fun countUpcomingRoutesByChildren(
        @Param("companyId") companyId: String,
        @Param("childIds") childIds: List<String>,
        @Param("fromDate") fromDate: LocalDate,
        @Param("statuses") statuses: Set<RouteStatus>
    ): Int
}