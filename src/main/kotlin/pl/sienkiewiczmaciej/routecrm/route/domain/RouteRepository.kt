package pl.sienkiewiczmaciej.routecrm.route.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
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
        endTime: LocalTime
    ): Boolean

    suspend fun hasVehicleConflict(
        companyId: CompanyId,
        vehicleId: VehicleId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Boolean
}

interface RouteChildRepository {
    suspend fun save(routeChild: RouteChild): RouteChild
    suspend fun findById(companyId: CompanyId, id: RouteChildId): RouteChild?
    suspend fun findByRoute(companyId: CompanyId, routeId: RouteId): List<RouteChild>
    suspend fun findByRouteAndChild(
        companyId: CompanyId,
        routeId: RouteId,
        childId: ChildId
    ): RouteChild?
    suspend fun countByRoute(companyId: CompanyId, routeId: RouteId): Int
    suspend fun deleteByRoute(companyId: CompanyId, routeId: RouteId)

    suspend fun hasChildConflict(
        companyId: CompanyId,
        childId: ChildId,
        date: LocalDate,
        pickupTime: LocalTime,
        dropoffTime: LocalTime
    ): Boolean
}

interface RouteNoteRepository {
    suspend fun save(note: RouteNote): RouteNote
    suspend fun findByRoute(companyId: CompanyId, routeId: RouteId): List<RouteNote>
    suspend fun deleteByRoute(companyId: CompanyId, routeId: RouteId)
}