package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Repository
class RouteRepositoryImpl(
    private val jpaRepository: RouteJpaRepository
) : RouteRepository {

    override suspend fun save(route: Route): Route = withContext(Dispatchers.IO) {
        val entity = RouteEntity.fromDomain(route)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: RouteId): Route? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findAll(
        companyId: CompanyId,
        date: LocalDate?,
        status: RouteStatus?,
        driverId: DriverId?,
        pageable: Pageable
    ): Page<Route> = withContext(Dispatchers.IO) {
        jpaRepository.findByFilters(
            companyId.value,
            date,
            status,
            driverId?.value,
            pageable
        ).map { it.toDomain() }
    }

    override suspend fun findByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate?,
        pageable: Pageable
    ): Page<Route> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndDriverIdAndDate(
            companyId.value,
            driverId.value,
            date,
            pageable
        ).map { it.toDomain() }
    }

    @Transactional
    override suspend fun delete(companyId: CompanyId, id: RouteId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}

@Repository
class RouteChildRepositoryImpl(
    private val jpaRepository: RouteChildJpaRepository
) : RouteChildRepository {

    override suspend fun save(routeChild: RouteChild): RouteChild = withContext(Dispatchers.IO) {
        val entity = RouteChildEntity.fromDomain(routeChild)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: RouteChildId): RouteChild? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByRoute(companyId: CompanyId, routeId: RouteId): List<RouteChild> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndRouteId(companyId.value, routeId.value)
                .map { it.toDomain() }
        }

    override suspend fun findByRouteAndChild(
        companyId: CompanyId,
        routeId: RouteId,
        childId: ChildId
    ): RouteChild? = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndRouteIdAndChildId(
            companyId.value,
            routeId.value,
            childId.value
        )?.toDomain()
    }

    override suspend fun countByRoute(companyId: CompanyId, routeId: RouteId): Int =
        withContext(Dispatchers.IO) {
            jpaRepository.countByCompanyIdAndRouteId(companyId.value, routeId.value)
        }

    @Transactional
    override suspend fun deleteByRoute(companyId: CompanyId, routeId: RouteId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByCompanyIdAndRouteId(companyId.value, routeId.value)
        }
    }
}

@Repository
class RouteNoteRepositoryImpl(
    private val jpaRepository: RouteNoteJpaRepository
) : RouteNoteRepository {

    override suspend fun save(note: RouteNote): RouteNote = withContext(Dispatchers.IO) {
        val entity = RouteNoteEntity.fromDomain(note)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findByRoute(companyId: CompanyId, routeId: RouteId): List<RouteNote> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndRouteIdOrderByCreatedAtDesc(
                companyId.value,
                routeId.value
            ).map { it.toDomain() }
        }

    @Transactional
    override suspend fun deleteByRoute(companyId: CompanyId, routeId: RouteId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByCompanyIdAndRouteId(companyId.value, routeId.value)
        }
    }
}