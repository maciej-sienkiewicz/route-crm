// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/RouteDriverAssignmentRepository.kt
package pl.sienkiewiczmaciej.routecrm.route.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

interface RouteDriverAssignmentRepository {
    suspend fun save(assignment: RouteDriverAssignment): RouteDriverAssignment

    suspend fun findByRoute(
        companyId: CompanyId,
        routeId: RouteId
    ): List<RouteDriverAssignment>

    suspend fun findOriginalDriver(
        companyId: CompanyId,
        routeId: RouteId
    ): DriverId?

    suspend fun findInDateRange(
        companyId: CompanyId,
        from: Instant,
        to: Instant,
        pageable: Pageable
    ): Page<RouteDriverAssignment>

    suspend fun findByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        from: Instant,
        to: Instant
    ): List<RouteDriverAssignment>

    suspend fun countByRoute(
        companyId: CompanyId,
        routeId: RouteId
    ): Int
}