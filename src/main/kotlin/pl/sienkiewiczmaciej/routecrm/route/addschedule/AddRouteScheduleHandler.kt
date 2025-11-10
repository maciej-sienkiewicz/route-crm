// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/addschedule/AddRouteScheduleHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.addschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.domain.services.RouteStopOrderingService
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
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
    private val validatorComposite: AddScheduleValidatorComposite,
    private val stopsFactory: AddRouteScheduleStopsFactory,
    private val orderingService: RouteStopOrderingService,
    private val stopRepository: RouteStopRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: AddRouteScheduleCommand): AddRouteScheduleResult {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Validate (throws exception on failure, returns context)
        val context = validatorComposite.validate(command)

        // 3. Reorder existing stops to make room for new ones
        val pickupPosition = command.pickupStop.stopOrder
        val existingStops = context.existingStops

        val reorderedStops = orderingService.insertStopsAt(
            existingStops = existingStops,
            insertPosition = pickupPosition,
            numberOfStopsToInsert = 2
        )

        if (reorderedStops.isNotEmpty()) {
            stopRepository.saveAll(reorderedStops)
        }

        // 4. Create pickup and dropoff stops
        val (pickupStop, dropoffStop) = stopsFactory.createScheduleStops(
            routeId = command.routeId,
            companyId = command.companyId,
            command = command,
            child = context.child,
            schedule = context.schedule,
            vehicle = context.vehicle
        )

        // 5. Persist both stops
        val savedPickup = stopRepository.save(pickupStop)
        val savedDropoff = stopRepository.save(dropoffStop)

        // 6. Return result
        return AddRouteScheduleResult(
            pickupStopId = savedPickup.id,
            dropoffStopId = savedDropoff.id,
            scheduleId = command.scheduleId,
            childId = command.childId
        )
    }
}