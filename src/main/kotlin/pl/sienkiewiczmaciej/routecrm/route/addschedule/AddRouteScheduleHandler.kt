// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/addschedule/AddRouteScheduleHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.addschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class RouteStopCreationData(
    val stopOrder: Int,
    val stopType: StopType,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress
)

data class AddRouteScheduleCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val pickupStop: RouteStopCreationData,
    val dropoffStop: RouteStopCreationData
)

data class AddRouteScheduleResult(
    val pickupStopId: RouteStopId,
    val dropoffStopId: RouteStopId,
    val scheduleId: ScheduleId,
    val childId: ChildId
)

@Component
class AddRouteScheduleHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val childRepository: ChildRepository,
    private val scheduleRepository: ScheduleRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: AddRouteScheduleCommand): AddRouteScheduleResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        require(route.status == RouteStatus.PLANNED) {
            "Cannot add schedule to route with status ${route.status}"
        }

        val child = childRepository.findById(command.companyId, command.childId)
            ?: throw ChildNotFoundException(command.childId)

        require(child.status == ChildStatus.ACTIVE) {
            "Child ${child.id.value} must be ACTIVE to be assigned to a route"
        }

        // Weryfikuj że schedule istnieje i należy do tego dziecka
        val schedule = scheduleRepository.findById(command.companyId, command.scheduleId)
            ?: throw IllegalArgumentException("Schedule ${command.scheduleId.value} not found")

        require(schedule.childId == command.childId) {
            "Schedule ${command.scheduleId.value} does not belong to child ${command.childId.value}"
        }

        // Sprawdź czy dziecko nie jest już w tej trasie
        val existingStops = stopRepository.findByRoute(command.companyId, command.routeId)
        val childAlreadyInRoute = existingStops.any { it.childId == command.childId && !it.isCancelled }

        require(!childAlreadyInRoute) {
            "Child ${command.childId.value} is already in this route"
        }

        // Walidacja poprawności stopOrder - pickup musi być przed dropoff
        require(command.pickupStop.stopOrder < command.dropoffStop.stopOrder) {
            "Pickup stop order (${command.pickupStop.stopOrder}) must be before dropoff stop order (${command.dropoffStop.stopOrder})"
        }

        // Reorder istniejących stopów jeśli potrzeba
        val stopsToShift = mutableListOf<RouteStop>()

        existingStops.forEach { stop ->
            if (stop.stopOrder >= command.pickupStop.stopOrder) {
                // Przesuń o 2 miejsca (pickup + dropoff)
                stopsToShift.add(stop.updateOrder(stop.stopOrder + 2))
            }
        }

        if (stopsToShift.isNotEmpty()) {
            stopRepository.saveAll(stopsToShift)
        }

        val pickupAddress = command.pickupStop.address.copy(
            latitude = schedule.pickupAddress.latitude,
            longitude = schedule.pickupAddress.longitude
        )

        val dropoffAddress = command.dropoffStop.address.copy(
            latitude = schedule.dropoffAddress.latitude,
            longitude = schedule.dropoffAddress.longitude
        )

        // Utwórz oba stopy
        val pickupStop = RouteStop.create(
            companyId = command.companyId,
            routeId = command.routeId,
            stopOrder = command.pickupStop.stopOrder,
            stopType = StopType.PICKUP,
            childId = command.childId,
            scheduleId = command.scheduleId,
            estimatedTime = command.pickupStop.estimatedTime,
            address = pickupAddress
        )

        val dropoffStop = RouteStop.create(
            companyId = command.companyId,
            routeId = command.routeId,
            stopOrder = command.dropoffStop.stopOrder,
            stopType = StopType.DROPOFF,
            childId = command.childId,
            scheduleId = command.scheduleId,
            estimatedTime = command.dropoffStop.estimatedTime,
            address = dropoffAddress
        )

        val savedPickup = stopRepository.save(pickupStop)
        val savedDropoff = stopRepository.save(dropoffStop)

        return AddRouteScheduleResult(
            pickupStopId = savedPickup.id,
            dropoffStopId = savedDropoff.id,
            scheduleId = command.scheduleId,
            childId = command.childId
        )
    }
}