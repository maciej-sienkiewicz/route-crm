// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/RouteRepository.kt
package pl.sienkiewiczmaciej.routecrm.route.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.LocalDate
import java.time.LocalTime

interface RouteRepository {
    suspend fun save(route: Route): Route
    suspend fun findById(companyId: CompanyId, id: RouteId): Route?
    suspend fun findAll(
        companyId: CompanyId,
        date: LocalDate?,
        status: RouteStatus?,
        driverId: DriverId?,
        pageable: Pageable
    ): Page<Route>
    suspend fun findByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate?,
        pageable: Pageable
    ): Page<Route>
    suspend fun delete(companyId: CompanyId, id: RouteId)
    suspend fun hasDriverConflict(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeRouteId: RouteId? = null
    ): Boolean
    suspend fun hasVehicleConflict(
        companyId: CompanyId,
        vehicleId: VehicleId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeRouteId: RouteId? = null
    ): Boolean
    suspend fun findBySchedule(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        statuses: Set<RouteStatus>,
        pageable: Pageable
    ): Page<Route>
    suspend fun findBySeries(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        fromDate: LocalDate,
        statuses: Set<RouteStatus>
    ): List<Route>
}

interface RouteStopRepository {
    suspend fun save(stop: RouteStop): RouteStop
    suspend fun saveAll(stops: List<RouteStop>): List<RouteStop>
    suspend fun findById(companyId: CompanyId, id: RouteStopId): RouteStop?
    suspend fun findByRoute(
        companyId: CompanyId,
        routeId: RouteId,
        includeCancelled: Boolean = false
    ): List<RouteStop>
    suspend fun delete(companyId: CompanyId, id: RouteStopId)
    suspend fun deleteByRoute(companyId: CompanyId, routeId: RouteId)
    suspend fun countByRoute(companyId: CompanyId, routeId: RouteId): Int
    suspend fun findByScheduleAndDate(companyId: CompanyId, scheduleId: ScheduleId, date: LocalDate): List<RouteStop>
    suspend fun findNextUnexecutedStop(companyId: CompanyId, routeId: RouteId): RouteStop?
}

interface RouteNoteRepository {
    suspend fun save(note: RouteNote): RouteNote
    suspend fun findByRoute(companyId: CompanyId, routeId: RouteId): List<RouteNote>
    suspend fun deleteByRoute(companyId: CompanyId, routeId: RouteId)
}