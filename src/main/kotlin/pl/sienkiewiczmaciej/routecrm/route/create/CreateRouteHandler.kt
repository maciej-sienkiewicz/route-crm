package pl.sienkiewiczmaciej.routecrm.route.create

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
import pl.sienkiewiczmaciej.routecrm.route.RoutePointRequest
import pl.sienkiewiczmaciej.routecrm.route.RoutePointType
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
    val points: List<RoutePointRequest>
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
    private val scheduleRepository: ScheduleRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateRouteCommand): CreateRouteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // Walidacja Driver i Vehicle (bez zmian)
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

        // Walidacja punktów
        require(command.points.isNotEmpty()) { "At least one point is required" }

        // Grupowanie punktów po dziecku
        val pointsByChild = command.points.groupBy { it.childId }

        // Walidacja: każde dziecko musi mieć PICKUP i DROPOFF
        pointsByChild.forEach { (childId, points) ->
            val hasPickup = points.any { it.type == RoutePointType.PICKUP }
            val hasDropoff = points.any { it.type == RoutePointType.DROPOFF }

            require(hasPickup && hasDropoff) {
                "Child $childId must have both PICKUP and DROPOFF points"
            }

            // Sprawdź czy są dokładnie 1 pickup i 1 dropoff
            require(points.count { it.type == RoutePointType.PICKUP } == 1) {
                "Child $childId must have exactly one PICKUP point"
            }
            require(points.count { it.type == RoutePointType.DROPOFF } == 1) {
                "Child $childId must have exactly one DROPOFF point"
            }
        }

        // Walidacja dzieci i przygotowanie danych
        val childrenData = pointsByChild.map { (childIdStr, points) ->
            val childId = ChildId.from(childIdStr)

            val child = childRepository.findById(command.companyId, childId)
                ?: throw ChildNotFoundException(childId)

            require(child.status == ChildStatus.ACTIVE) {
                "Child ${child.id.value} must be ACTIVE to be assigned to a route"
            }

            val pickupPoint = points.first { it.type == RoutePointType.PICKUP }
            val dropoffPoint = points.first { it.type == RoutePointType.DROPOFF }

            // Walidacja czasów
            require(dropoffPoint.estimatedTime.isAfter(pickupPoint.estimatedTime)) {
                "Dropoff time must be after pickup time for child ${childIdStr}"
            }

            // Pobierz harmonogram aby uzyskać adresy
            val scheduleId = ScheduleId.from(pickupPoint.scheduleId)
            val schedule = scheduleRepository.findById(
                companyId = command.companyId,
                id = scheduleId
            ) ?: throw IllegalArgumentException("Schedule ${scheduleId.value} not found")

            require(schedule.childId == childId) {
                "Schedule ${scheduleId.value} does not belong to child ${childId.value}"
            }

            // Sprawdź konflikty czasowe dla dziecka
            if (routeChildRepository.hasChildConflict(
                    command.companyId,
                    childId,
                    command.date,
                    pickupPoint.estimatedTime,
                    dropoffPoint.estimatedTime
                )) {
                throw IllegalArgumentException(
                    "Child ${childId.value} already has a route at this time on ${command.date}"
                )
            }

            Triple(child, pickupPoint, dropoffPoint)
        }

        // Walidacja pojemności pojazdu
        val wheelchairCount = childrenData.count { (child, _, _) ->
            child.transportNeeds.wheelchair
        }
        require(wheelchairCount <= vehicle.capacity.wheelchairSpaces) {
            "Number of children requiring wheelchair ($wheelchairCount) exceeds vehicle wheelchair capacity (${vehicle.capacity.wheelchairSpaces})"
        }

        val specialSeatCount = childrenData.count { (child, _, _) ->
            child.transportNeeds.specialSeat
        }
        require(specialSeatCount <= vehicle.capacity.childSeats) {
            "Number of children requiring special seats ($specialSeatCount) exceeds vehicle special seat capacity (${vehicle.capacity.childSeats})"
        }

        require(childrenData.size <= vehicle.capacity.totalSeats) {
            "Number of children (${childrenData.size}) exceeds vehicle capacity (${vehicle.capacity.totalSeats})"
        }

        // Utwórz trasę
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

        // Utwórz RouteChild dla każdego dziecka
        childrenData.forEach { (child, pickupPoint, dropoffPoint) ->
            val scheduleId = ScheduleId.from(pickupPoint.scheduleId)
            val schedule = scheduleRepository.findById(
                companyId = command.companyId,
                id = scheduleId
            )!!

            val routeChild = RouteChild.create(
                companyId = command.companyId,
                routeId = savedRoute.id,
                childId = child.id,
                scheduleId = scheduleId,
                pickupOrder = pickupPoint.order,
                pickupAddress = schedule.pickupAddress,
                dropoffAddress = schedule.dropoffAddress,
                estimatedPickupTime = pickupPoint.estimatedTime,
                estimatedDropoffTime = dropoffPoint.estimatedTime
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
            childrenCount = childrenData.size
        )
    }
}