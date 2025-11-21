// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteDriverAssignmentRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteDriverAssignment
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteDriverAssignmentRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

@Repository
class RouteDriverAssignmentRepositoryImpl(
    private val jpaRepository: RouteDriverAssignmentJpaRepository
) : RouteDriverAssignmentRepository {

    override suspend fun save(assignment: RouteDriverAssignment): RouteDriverAssignment =
        withContext(Dispatchers.IO) {
            val entity = RouteDriverAssignmentEntity.fromDomain(assignment)
            jpaRepository.save(entity).toDomain()
        }

    override suspend fun findByRoute(
        companyId: CompanyId,
        routeId: RouteId
    ): List<RouteDriverAssignment> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndRouteIdOrderByReassignedAtAsc(
            companyId.value,
            routeId.value
        ).map { it.toDomain() }
    }

    override suspend fun findOriginalDriver(
        companyId: CompanyId,
        routeId: RouteId
    ): DriverId? = withContext(Dispatchers.IO) {
        jpaRepository.findFirstByRoute(
            companyId.value,
            routeId.value
        )?.previousDriverId?.let { DriverId(it) }
    }

    override suspend fun findInDateRange(
        companyId: CompanyId,
        from: Instant,
        to: Instant,
        pageable: Pageable
    ): Page<RouteDriverAssignment> = withContext(Dispatchers.IO) {
        jpaRepository.findInDateRange(
            companyId.value,
            from,
            to,
            pageable
        ).map { it.toDomain() }
    }

    override suspend fun findByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        from: Instant,
        to: Instant
    ): List<RouteDriverAssignment> = withContext(Dispatchers.IO) {
        jpaRepository.findByDriver(
            companyId.value,
            driverId.value,
            from,
            to
        ).map { it.toDomain() }
    }

    override suspend fun countByRoute(
        companyId: CompanyId,
        routeId: RouteId
    ): Int = withContext(Dispatchers.IO) {
        jpaRepository.countByCompanyIdAndRouteId(
            companyId.value,
            routeId.value
        )
    }
}