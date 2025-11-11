// route/cancelschedule/CancelRouteScheduleHandler.kt (UPDATED WITH EVENTS)
package pl.sienkiewiczmaciej.routecrm.route.cancelschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteScheduleCancelledEvent
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class CancelRouteScheduleCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val scheduleId: ScheduleId,
    val reason: String
)

data class CancelRouteScheduleResult(
    val scheduleId: ScheduleId,
    val pickupStopId: RouteStopId,
    val dropoffStopId: RouteStopId,
    val cancelledStopsCount: Int,
    val cancelledAt: Instant
)

@Component
class CancelRouteScheduleHandler(
    private val validatorComposite: CancelScheduleValidatorComposite,
    private val stopRepository: RouteStopRepository,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CancelRouteScheduleCommand): CancelRouteScheduleResult {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Validate (throws exception on failure, returns context)
        val context = validatorComposite.validate(command)

        // 3. Find pickup and dropoff stops
        val pickupStop = context.scheduleStops.first { it.stopType == StopType.PICKUP }
        val dropoffStop = context.scheduleStops.first { it.stopType == StopType.DROPOFF }

        // 4. Cancel both stops using domain method
        val cancelledPickup = pickupStop.cancel(command.reason)
        val cancelledDropoff = dropoffStop.cancel(command.reason)

        // 5. Persist cancelled stops
        stopRepository.save(cancelledPickup)
        stopRepository.save(cancelledDropoff)

        // 6. Publish event
        eventPublisher.publish(
            RouteScheduleCancelledEvent(
                aggregateId = command.routeId.value,
                routeId = command.routeId,
                scheduleId = command.scheduleId,
                pickupStopId = cancelledPickup.id,
                dropoffStopId = cancelledDropoff.id,
                cancelledBy = principal.userId,
                reason = command.reason,
                companyId = principal.companyId
            )
        )

        // 7. Return result
        return CancelRouteScheduleResult(
            scheduleId = command.scheduleId,
            pickupStopId = cancelledPickup.id,
            dropoffStopId = cancelledDropoff.id,
            cancelledStopsCount = 2,
            cancelledAt = Instant.now()
        )
    }
}