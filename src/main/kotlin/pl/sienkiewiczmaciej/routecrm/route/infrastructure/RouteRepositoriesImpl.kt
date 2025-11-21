// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteRepositoriesImpl.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.LocalDate
import java.time.LocalTime

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

    override suspend fun findBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        fromDate: LocalDate,
        statuses: Set<RouteStatus>
    ): List<Route> = withContext(Dispatchers.IO) {
        jpaRepository.findBySeriesFromDate(
            companyId.value,
            seriesId.value,
            fromDate,
            statuses
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

    override suspend fun hasDriverConflict(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeRouteId: RouteId?
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsDriverConflict(
            companyId.value,
            driverId.value,
            date,
            startTime,
            endTime,
            excludeRouteId?.value
        )
    }

    override suspend fun hasVehicleConflict(
        companyId: CompanyId,
        vehicleId: VehicleId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeRouteId: RouteId?
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsVehicleConflict(
            companyId.value,
            vehicleId.value,
            date,
            startTime,
            endTime,
            excludeRouteId?.value
        )
    }

    override suspend fun findBySchedule(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        statuses: Set<RouteStatus>,
        pageable: Pageable
    ): Page<Route> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndScheduleIdAndStatuses(
            companyId.value,
            scheduleId.value,
            statuses,
            pageable
        ).map { it.toDomain() }
    }
}

@Repository
class RouteStopRepositoryImpl(
    private val jpaRepository: RouteStopJpaRepository
) : RouteStopRepository {

    override suspend fun save(stop: RouteStop): RouteStop = withContext(Dispatchers.IO) {
        val entity = RouteStopEntity.fromDomain(stop)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun saveAll(stops: List<RouteStop>): List<RouteStop> =
        withContext(Dispatchers.IO) {
            val entities = stops.map { RouteStopEntity.fromDomain(it) }
            jpaRepository.saveAll(entities).map { it.toDomain() }
        }

    override suspend fun findById(companyId: CompanyId, id: RouteStopId): RouteStop? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByRoute(
        companyId: CompanyId,
        routeId: RouteId,
        includeCancelled: Boolean
    ): List<RouteStop> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndRouteId(
            companyId.value,
            routeId.value,
            includeCancelled
        ).map { it.toDomain() }
    }

    @Transactional
    override suspend fun delete(companyId: CompanyId, id: RouteStopId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }

    @Transactional
    override suspend fun deleteByRoute(companyId: CompanyId, routeId: RouteId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByCompanyIdAndRouteId(companyId.value, routeId.value)
        }
    }

    override suspend fun countByRoute(companyId: CompanyId, routeId: RouteId): Int =
        withContext(Dispatchers.IO) {
            jpaRepository.countByCompanyIdAndRouteId(companyId.value, routeId.value)
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