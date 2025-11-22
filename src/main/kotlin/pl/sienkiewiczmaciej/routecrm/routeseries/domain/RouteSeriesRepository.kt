// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/domain/RouteSeriesRepository.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface RouteSeriesRepository {
    suspend fun save(series: RouteSeries): RouteSeries
    suspend fun findById(companyId: CompanyId, id: RouteSeriesId): RouteSeries?
    suspend fun findAll(
        companyId: CompanyId,
        status: RouteSeriesStatus?,
        pageable: Pageable
    ): Page<RouteSeries>
    suspend fun findActive(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<RouteSeries>
    suspend fun delete(companyId: CompanyId, id: RouteSeriesId)
}

interface RouteSeriesScheduleRepository {
    suspend fun save(schedule: RouteSeriesSchedule): RouteSeriesSchedule
    suspend fun saveAll(schedules: List<RouteSeriesSchedule>): List<RouteSeriesSchedule>
    suspend fun findById(
        companyId: CompanyId,
        id: RouteSeriesScheduleId
    ): RouteSeriesSchedule?
    suspend fun findBySeriesAndSchedule(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        scheduleId: ScheduleId
    ): RouteSeriesSchedule?
    suspend fun findActiveBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        date: LocalDate
    ): List<RouteSeriesSchedule>
    suspend fun findAllBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId
    ): List<RouteSeriesSchedule>
    suspend fun delete(companyId: CompanyId, id: RouteSeriesScheduleId)
    /**
     * Finds all series schedules for a given schedule ID.
     * Used to detect if schedule is already part of any series.
     */
    suspend fun findBySchedule(
        companyId: CompanyId,
        scheduleId: ScheduleId
    ): List<RouteSeriesSchedule>
}

interface RouteSeriesOccurrenceRepository {
    suspend fun save(occurrence: RouteSeriesOccurrence): RouteSeriesOccurrence
    suspend fun saveAll(occurrences: List<RouteSeriesOccurrence>): List<RouteSeriesOccurrence>
    suspend fun findBySeriesAndDate(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        date: LocalDate
    ): RouteSeriesOccurrence?
    suspend fun findBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<RouteSeriesOccurrence>
}