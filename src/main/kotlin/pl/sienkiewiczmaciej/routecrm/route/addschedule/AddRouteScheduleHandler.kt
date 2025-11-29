package pl.sienkiewiczmaciej.routecrm.route.addschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteScheduleAddedEvent
import pl.sienkiewiczmaciej.routecrm.route.domain.services.StopInsertionService
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class RouteStopCreationData(
    val stopOrder: Int,
    val stopType: pl.sienkiewiczmaciej.routecrm.route.domain.StopType,
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
    private val insertionService: StopInsertionService,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: AddRouteScheduleCommand): AddRouteScheduleResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val context = validatorComposite.validate(command)

        val (pickupStop, dropoffStop) = stopsFactory.createScheduleStops(
            routeId = command.routeId,
            companyId = command.companyId,
            command = command,
            child = context.child,
            schedule = context.schedule,
            vehicle = context.vehicle
        )

        val existingStops = context.existingStops
            .filterNot { it.isCancelled }
            .sortedBy { it.stopOrder }

        val displayOrderToInsertAfter = command.pickupStop.stopOrder - 1

        val afterOrder = if (existingStops.isEmpty()) {
            null
        } else if (displayOrderToInsertAfter <= 0) {
            null
        } else if (displayOrderToInsertAfter >= existingStops.size) {
            existingStops.last().stopOrder
        } else {
            existingStops[displayOrderToInsertAfter].stopOrder
        }

        val result = insertionService.insertStops(
            companyId = command.companyId,
            routeId = command.routeId,
            stopsToInsert = listOf(pickupStop, dropoffStop),
            afterOrder = afterOrder
        )

        val savedPickup = result.insertedStops[0]
        val savedDropoff = result.insertedStops[1]

        eventPublisher.publish(
            RouteScheduleAddedEvent(
                aggregateId = command.routeId.value,
                routeId = command.routeId,
                scheduleId = command.scheduleId,
                child = context.child,
                pickupStop = savedPickup,
                dropoffStop = savedDropoff,
                addedBy = principal.userId,
                companyId = principal.companyId,
                routeDate = context.route.date
            )
        )

        return AddRouteScheduleResult(
            pickupStopId = savedPickup.id,
            dropoffStopId = savedDropoff.id,
            scheduleId = command.scheduleId,
            childId = command.childId
        )
    }
}