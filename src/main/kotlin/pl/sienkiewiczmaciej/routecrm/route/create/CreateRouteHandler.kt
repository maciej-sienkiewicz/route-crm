// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/create/CreateRouteHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.create

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleStatus
import pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.VehicleNotFoundException
import java.time.LocalDate
import java.time.LocalTime

data class RouteStopData(
    val stopOrder: Int,
    val stopType: StopType,
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress
)

data class CreateRouteCommand(
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val stops: List<RouteStopData>
)

data class CreateRouteResult(
    val id: RouteId,
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val stopsCount: Int
)

@Component
@JsonIgnoreProperties
class CreateRouteHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val childRepository: ChildRepository,
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository,
    private val scheduleRepository: ScheduleRepository,
    private val authService: AuthorizationService,
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateRouteCommand): CreateRouteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        require(command.stops.isNotEmpty()) { "At least one stop is required" }

        val stopOrders = command.stops.map { it.stopOrder }.sorted()
        require(stopOrders == (1..stopOrders.size).toList()) {
            "Stop orders must be consecutive starting from 1"
        }

        val driver = driverRepository.findById(command.companyId, command.driverId)
            ?: throw DriverNotFoundException(command.driverId)

        require(driver.status == DriverStatus.ACTIVE) {
            "Driver must be ACTIVE to be assigned to a route"
        }

        if (routeRepository.hasDriverConflict(
                command.companyId,
                command.driverId,
                command.date,
                command.estimatedStartTime,
                command.estimatedEndTime
            )) {
            throw IllegalArgumentException(
                "Driver ${command.driverId.value} already has a route at this time on ${command.date}"
            )
        }

        val vehicle = vehicleRepository.findById(command.companyId, command.vehicleId)
            ?: throw VehicleNotFoundException(command.vehicleId)

        require(vehicle.status == VehicleStatus.AVAILABLE) {
            "Vehicle must be AVAILABLE to be assigned to a route"
        }

        if (routeRepository.hasVehicleConflict(
                command.companyId,
                command.vehicleId,
                command.date,
                command.estimatedStartTime,
                command.estimatedEndTime
            )) {
            throw IllegalArgumentException(
                "Vehicle ${command.vehicleId.value} is already assigned to another route at this time on ${command.date}"
            )
        }

        val childrenIds = command.stops.map { it.childId }.toSet()
        val children = childrenIds.associateWith { childId ->
            childRepository.findById(command.companyId, childId)
                ?: throw ChildNotFoundException(childId)
        }

        children.values.forEach { child ->
            require(child.status == ChildStatus.ACTIVE) {
                "Child ${child.id.value} must be ACTIVE to be assigned to a route"
            }
        }

        val childrenInStops = command.stops.map { children[it.childId]!! }
        val wheelchairCount = childrenInStops.distinctBy { it.id }.count { it.transportNeeds.wheelchair }
        require(wheelchairCount <= vehicle.capacity.wheelchairSpaces) {
            "Number of children requiring wheelchair ($wheelchairCount) exceeds vehicle wheelchair capacity (${vehicle.capacity.wheelchairSpaces})"
        }

        val specialSeatCount = childrenInStops.distinctBy { it.id }.count { it.transportNeeds.specialSeat }
        require(specialSeatCount <= vehicle.capacity.childSeats) {
            "Number of children requiring special seats ($specialSeatCount) exceeds vehicle special seat capacity (${vehicle.capacity.childSeats})"
        }

        val uniqueChildrenCount = childrenIds.size
        require(uniqueChildrenCount <= vehicle.capacity.totalSeats) {
            "Number of children ($uniqueChildrenCount) exceeds vehicle capacity (${vehicle.capacity.totalSeats})"
        }

        val route = Route.create(
            companyId = command.companyId,
            routeName = command.routeName,
            date = command.date,
            driverId = command.driverId,
            vehicleId = command.vehicleId,
            estimatedStartTime = command.estimatedStartTime,
            estimatedEndTime = command.estimatedEndTime
        )

        val savedRoute = routeRepository.save(route)

        val stops = command.stops.map { stopData ->
            val schedule = scheduleRepository.findById(principal.companyId, stopData.scheduleId)
            RouteStop.create(
                companyId = command.companyId,
                routeId = savedRoute.id,
                stopOrder = stopData.stopOrder,
                stopType = stopData.stopType,
                childId = stopData.childId,
                scheduleId = stopData.scheduleId,
                estimatedTime = stopData.estimatedTime,
                address = stopData.address
                    .copy(latitude = if(stopData.stopType == StopType.PICKUP) schedule!!.pickupAddress.latitude else schedule!!.dropoffAddress.latitude,
                        longitude = if(stopData.stopType == StopType.PICKUP) schedule!!.pickupAddress.longitude else schedule.dropoffAddress.longitude)
            )
        }

        stopRepository.saveAll(stops)

        return CreateRouteResult(
            id = savedRoute.id,
            companyId = savedRoute.companyId,
            routeName = savedRoute.routeName,
            date = savedRoute.date,
            status = savedRoute.status,
            driverId = savedRoute.driverId,
            vehicleId = savedRoute.vehicleId,
            estimatedStartTime = savedRoute.estimatedStartTime,
            estimatedEndTime = savedRoute.estimatedEndTime,
            stopsCount = stops.size
        )
    }
}