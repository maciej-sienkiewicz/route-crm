// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/reorderstops/ReorderRouteStopsHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.reorderstops

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
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
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: ReorderRouteStopsCommand
    ): ReorderRouteStopsResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        require(route.status == RouteStatus.PLANNED) {
            "Cannot reorder stops in route with status ${route.status}"
        }

        val existingStops = stopRepository.findByRoute(command.companyId, command.routeId)

        val stopIdToStopMap = existingStops.associateBy { it.id }

        command.stopOrders.forEach { update ->
            require(stopIdToStopMap.containsKey(update.stopId)) {
                "Stop ${update.stopId.value} not found in route"
            }
            require(update.newOrder > 0) {
                "Stop order must be positive"
            }
        }

        val newOrders = command.stopOrders.map { it.newOrder }
        require(newOrders.toSet().size == newOrders.size) {
            "Duplicate stop orders are not allowed"
        }

        val stopsToUpdate = command.stopOrders.map { update ->
            val stop = stopIdToStopMap[update.stopId]!!
            require(stop.canBeModified()) {
                "Cannot reorder stop ${stop.id.value}: ${
                    when {
                        stop.isExecuted() -> "already executed"
                        stop.isCancelled -> "cancelled"
                        else -> "not modifiable"
                    }
                }"
            }
            stop.updateOrder(update.newOrder)
        }

        stopRepository.saveAll(stopsToUpdate)

        return ReorderRouteStopsResult(
            routeId = command.routeId,
            updatedStopsCount = stopsToUpdate.size
        )
    }
}