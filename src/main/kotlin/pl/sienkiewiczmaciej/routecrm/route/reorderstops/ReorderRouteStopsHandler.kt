package pl.sienkiewiczmaciej.routecrm.route.reorderstops

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteStopsReorderedEvent
import pl.sienkiewiczmaciej.routecrm.route.domain.services.GapBasedStopOrderCalculator
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
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val context = validatorComposite.validate(command)

        val existingStops = context.existingStops.sortedBy { it.stopOrder }

        val stopIdToLogicalOrder = mutableMapOf<RouteStopId, Int>()

        command.stopOrders.sortedBy { it.newOrder }.forEachIndexed { index, update ->
            stopIdToLogicalOrder[update.stopId] = (index + 1) * GapBasedStopOrderCalculator.GAP_SIZE
        }

        val stopsWithNewOrder = existingStops.map { stop ->
            val newLogicalOrder = stopIdToLogicalOrder[stop.id] ?: stop.stopOrder
            stop.updateOrder(newLogicalOrder)
        }

        val stopsWithTemporaryOrder = stopsWithNewOrder.mapIndexed { index, stop ->
            stop.updateOrder(-(index + 1))
        }
        stopRepository.saveAll(stopsWithTemporaryOrder)

        val refreshedStops = stopRepository.findByRoute(command.companyId, command.routeId)
        val stopsWithFinalOrder = refreshedStops
            .sortedBy { it.stopOrder }
            .map { stop ->
                val finalLogicalOrder = stopIdToLogicalOrder[stop.id] ?: stop.stopOrder
                if (finalLogicalOrder < 0) {
                    stop.updateOrder(-finalLogicalOrder)
                } else {
                    stop.updateOrder(finalLogicalOrder)
                }
            }
        stopRepository.saveAll(stopsWithFinalOrder)

        eventPublisher.publish(
            RouteStopsReorderedEvent(
                aggregateId = command.routeId.value,
                routeId = command.routeId,
                reorderedBy = principal.userId,
                stopsCount = stopsWithFinalOrder.size,
                companyId = principal.companyId
            )
        )

        return ReorderRouteStopsResult(
            routeId = command.routeId,
            updatedStopsCount = stopsWithFinalOrder.size
        )
    }
}