// route/reorderstops/ReorderRouteStopsHandler.kt (UPDATED WITH EVENTS)
package pl.sienkiewiczmaciej.routecrm.route.reorderstops

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteStopsReorderedEvent
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class StopOrderUpdate(
    val stopId: RouteStopId,
    val newOrder: Int
)

data class ReorderRouteStopsCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopOrders: List<StopOrderUpdate>
)

data class ReorderRouteStopsResult(
    val routeId: RouteId,
    val updatedStopsCount: Int
)

@Component
class ReorderRouteStopsHandler(
    private val validatorComposite: ReorderStopsValidatorComposite,
    private val stopRepository: RouteStopRepository,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: ReorderRouteStopsCommand
    ): ReorderRouteStopsResult {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Validate (throws exception on failure, returns context)
        val context = validatorComposite.validate(command)

        // 3. Two-phase reorder to avoid unique constraint violations
        // First pass: assign temporary negative orders
        val stopsWithTemporaryOrder = command.stopOrders.mapIndexed { index, update ->
            val stop = context.existingStops.first { it.id == update.stopId }
            stop.updateOrder(-(index + 1))
        }
        stopRepository.saveAll(stopsWithTemporaryOrder)

        // Second pass: assign final orders
        val refreshedStops = stopRepository.findByRoute(command.companyId, command.routeId)
        val stopsWithFinalOrder = command.stopOrders.map { update ->
            val stop = refreshedStops.first { it.id == update.stopId }
            stop.updateOrder(update.newOrder)
        }
        stopRepository.saveAll(stopsWithFinalOrder)

        // 4. Publish event
        eventPublisher.publish(
            RouteStopsReorderedEvent(
                aggregateId = command.routeId.value,
                routeId = command.routeId,
                reorderedBy = principal.userId,
                stopsCount = stopsWithFinalOrder.size,
                companyId = principal.companyId
            )
        )

        // 5. Return result
        return ReorderRouteStopsResult(
            routeId = command.routeId,
            updatedStopsCount = stopsWithFinalOrder.size
        )
    }
}