package pl.sienkiewiczmaciej.routecrm.route.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
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

data class RouteChildData(
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val pickupOrder: Int,
    val pickupAddress: ScheduleAddress,
    val dropoffAddress: ScheduleAddress,
    val estimatedPickupTime: LocalTime,
    val estimatedDropoffTime: LocalTime
)

data class CreateRouteCommand(
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val children: List<RouteChildData>
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
    val childrenCount: Int
)

@Component
class CreateRouteHandler(
    private val routeRepository: RouteRepository,
    private val routeChildRepository: RouteChildRepository,
    private val childRepository: ChildRepository,
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateRouteCommand): CreateRouteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val driver = driverRepository.findById(command.companyId, command.driverId)
            ?: throw DriverNotFoundException(command.driverId)

        require(driver.status == DriverStatus.ACTIVE) {
            "Driver must be ACTIVE to be assigned to a route"
        }

        val vehicle = vehicleRepository.findById(command.companyId, command.vehicleId)
            ?: throw VehicleNotFoundException(command.vehicleId)

        require(vehicle.status == VehicleStatus.AVAILABLE) {
            "Vehicle must be AVAILABLE to be assigned to a route"
        }

        require(command.children.size <= vehicle.capacity.totalSeats) {
            "Number of children (${command.children.size}) exceeds vehicle capacity (${vehicle.capacity.totalSeats})"
        }

        command.children.forEach { childData ->
            val child = childRepository.findById(command.companyId, childData.childId)
                ?: throw ChildNotFoundException(childData.childId)
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

        command.children.forEach { childData ->
            val routeChild = RouteChild.create(
                companyId = command.companyId,
                routeId = savedRoute.id,
                childId = childData.childId,
                scheduleId = childData.scheduleId,
                pickupOrder = childData.pickupOrder,
                pickupAddress = childData.pickupAddress,
                dropoffAddress = childData.dropoffAddress,
                estimatedPickupTime = childData.estimatedPickupTime,
                estimatedDropoffTime = childData.estimatedDropoffTime
            )
            routeChildRepository.save(routeChild)
        }

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
            childrenCount = command.children.size
        )
    }
}