// route/deleteschedule/DeleteRouteScheduleHandler.kt (UPDATED WITH EVENTS)
package pl.sienkiewiczmaciej.routecrm.route.deleteschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteScheduleDeletedEvent
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteRouteScheduleCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val scheduleId: ScheduleId
)

@Component
class DeleteRouteScheduleHandler(
    private val validatorComposite: DeleteScheduleValidatorComposite,
    private val stopRepository: RouteStopRepository,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteRouteScheduleCommand) {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Validate (throws exception on failure, returns context)
        val context = validatorComposite.validate(command)

        // 3. Delete schedule stops
        context.scheduleStops.forEach { stop ->
            stopRepository.delete(command.companyId, stop.id)
        }

        // 4. Reorder remaining stops
        val remainingStops = stopRepository.findByRoute(command.companyId, command.routeId)

        if (remainingStops.isNotEmpty()) {
            val reorderedStops = remainingStops
                .sortedBy { it.stopOrder }
                .mapIndexed { index, stop -> stop.updateOrder(index + 1) }

            stopRepository.saveAll(reorderedStops)
        }

        // 5. Publish event
        eventPublisher.publish(
            RouteScheduleDeletedEvent(
                aggregateId = command.routeId.value,
                routeId = command.routeId,
                scheduleId = command.scheduleId,
                deletedBy = principal.userId,
                companyId = principal.companyId,
                child = context.child
            )
        )
    }
}