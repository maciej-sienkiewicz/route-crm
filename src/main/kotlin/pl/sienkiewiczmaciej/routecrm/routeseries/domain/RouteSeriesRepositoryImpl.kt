// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/infrastructure/RouteSeriesRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Repository
class RouteSeriesRepositoryImpl(
    private val jpaRepository: RouteSeriesJpaRepository
) : RouteSeriesRepository {

    override suspend fun save(series: RouteSeries): RouteSeries = withContext(Dispatchers.IO) {
        val entity = RouteSeriesEntity.fromDomain(series)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: RouteSeriesId): RouteSeries? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findAll(
        companyId: CompanyId,
        status: RouteSeriesStatus?,
        pageable: Pageable
    ): Page<RouteSeries> = withContext(Dispatchers.IO) {
        val page = if (status != null) {
            jpaRepository.findByCompanyIdAndStatus(companyId.value, status, pageable)
        } else {
            jpaRepository.findByCompanyId(companyId.value, pageable)
        }
        page.map { it.toDomain() }
    }

    override suspend fun findActive(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<RouteSeries> = withContext(Dispatchers.IO) {
        jpaRepository.findActiveInDateRange(
            companyId.value,
            startDate,
            endDate
        ).map { it.toDomain() }
    }

    @Transactional
    override suspend fun delete(companyId: CompanyId, id: RouteSeriesId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}

@Repository
class RouteSeriesScheduleRepositoryImpl(
    private val jpaRepository: RouteSeriesScheduleJpaRepository
) : RouteSeriesScheduleRepository {

    override suspend fun save(schedule: RouteSeriesSchedule): RouteSeriesSchedule =
        withContext(Dispatchers.IO) {
            val entity = RouteSeriesScheduleEntity.fromDomain(schedule)
            jpaRepository.save(entity).toDomain()
        }

    override suspend fun saveAll(schedules: List<RouteSeriesSchedule>): List<RouteSeriesSchedule> =
        withContext(Dispatchers.IO) {
            val entities = schedules.map { RouteSeriesScheduleEntity.fromDomain(it) }
            jpaRepository.saveAll(entities).map { it.toDomain() }
        }

    override suspend fun findById(
        companyId: CompanyId,
        id: RouteSeriesScheduleId
    ): RouteSeriesSchedule? = withContext(Dispatchers.IO) {
        jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
    }

    override suspend fun findBySeriesAndSchedule(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        scheduleId: ScheduleId
    ): RouteSeriesSchedule? = withContext(Dispatchers.IO) {
        jpaRepository.findActiveBySeriesAndScheduleOnDate(
            companyId.value,
            seriesId.value,
            scheduleId.value,
            LocalDate.now()
        )?.toDomain()
    }

    override suspend fun findActiveBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        date: LocalDate
    ): List<RouteSeriesSchedule> = withContext(Dispatchers.IO) {
        jpaRepository.findActiveBySeriesOnDate(
            companyId.value,
            seriesId.value,
            date
        ).map { it.toDomain() }
    }

    override suspend fun findAllBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId
    ): List<RouteSeriesSchedule> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndSeriesId(
            companyId.value,
            seriesId.value
        ).map { it.toDomain() }
    }

    @Transactional
    override suspend fun delete(companyId: CompanyId, id: RouteSeriesScheduleId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}

@Repository
class RouteSeriesOccurrenceRepositoryImpl(
    private val jpaRepository: RouteSeriesOccurrenceJpaRepository
) : RouteSeriesOccurrenceRepository {

    override suspend fun save(occurrence: RouteSeriesOccurrence): RouteSeriesOccurrence =
        withContext(Dispatchers.IO) {
            val entity = RouteSeriesOccurrenceEntity.fromDomain(occurrence)
            jpaRepository.save(entity).toDomain()
        }

    override suspend fun saveAll(occurrences: List<RouteSeriesOccurrence>): List<RouteSeriesOccurrence> =
        withContext(Dispatchers.IO) {
            val entities = occurrences.map { RouteSeriesOccurrenceEntity.fromDomain(it) }
            jpaRepository.saveAll(entities).map { it.toDomain() }
        }

    override suspend fun findBySeriesAndDate(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        date: LocalDate
    ): RouteSeriesOccurrence? = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndSeriesIdAndOccurrenceDate(
            companyId.value,
            seriesId.value,
            date
        )?.toDomain()
    }

    override suspend fun findBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<RouteSeriesOccurrence> = withContext(Dispatchers.IO) {
        jpaRepository.findBySeriesInDateRange(
            companyId.value,
            seriesId.value,
            fromDate,
            toDate
        ).map { it.toDomain() }
    }
}