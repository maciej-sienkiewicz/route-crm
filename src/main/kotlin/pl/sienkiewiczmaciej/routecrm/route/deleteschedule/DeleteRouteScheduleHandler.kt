// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/deleteschedule/DeleteRouteScheduleHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.deleteschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteRouteScheduleCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val scheduleId: ScheduleId
)

@Component
class DeleteRouteScheduleHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteRouteScheduleCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        require(route.canDeleteStops()) {
            "Cannot delete schedule from route with status ${route.status}"
        }

        // Znajdź wszystkie stopy dla tego schedule
        val allStops = stopRepository.findByRoute(command.companyId, command.routeId)
        val scheduleStops = allStops.filter { it.scheduleId == command.scheduleId }

        require(scheduleStops.isNotEmpty()) {
            "Schedule ${command.scheduleId.value} not found in route ${command.routeId.value}"
        }

        // Waliduj że wszystkie stopy mogą być usunięte
        scheduleStops.forEach { stop ->
            require(stop.canBeDeleted()) {
                "Cannot delete schedule: stop ${stop.id.value} (${stop.stopType}) has already been executed"
            }
        }

        // Usuń wszystkie stopy dla tego schedule (zazwyczaj 2: pickup i dropoff)
        scheduleStops.forEach { stop ->
            stopRepository.delete(command.companyId, stop.id)
        }

        // Przenumeruj pozostałe stopy
        val remainingStops = stopRepository.findByRoute(command.companyId, command.routeId)
        val reorderedStops = remainingStops
            .sortedBy { it.stopOrder }
            .mapIndexed { index, stop -> stop.updateOrder(index + 1) }

        if (reorderedStops.isNotEmpty()) {
            stopRepository.saveAll(reorderedStops)
        }
    }
}