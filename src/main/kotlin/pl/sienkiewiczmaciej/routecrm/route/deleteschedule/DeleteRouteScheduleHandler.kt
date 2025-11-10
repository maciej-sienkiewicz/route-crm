// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/deleteschedule/DeleteRouteScheduleHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.deleteschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.*
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
    private val validatorComposite: DeleteScheduleValidatorComposite,
    private val stopRepository: RouteStopRepository,
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

        // 4. Reorder remaining stops using domain service
        val remainingStops = stopRepository.findByRoute(command.companyId, command.routeId)

        if (remainingStops.isNotEmpty()) {
            val reorderedStops = remainingStops
                .sortedBy { it.stopOrder }
                .mapIndexed { index, stop -> stop.updateOrder(index + 1) }

            stopRepository.saveAll(reorderedStops)
        }
    }
}