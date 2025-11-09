// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/addstop/AddRouteStopHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.addstop

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
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class AddRouteStopCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopOrder: Int,
    val stopType: StopType,
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress
)

data class AddRouteStopResult(
    val id: RouteStopId,
    val routeId: RouteId,
    val stopOrder: Int,
    val stopType: StopType,
    val childId: ChildId
)

@Component
class AddRouteStopHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val childRepository: ChildRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: AddRouteStopCommand): AddRouteStopResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        require(route.status == RouteStatus.PLANNED) {
            "Cannot add stops to route with status ${route.status}"
        }

        val child = childRepository.findById(command.companyId, command.childId)
            ?: throw ChildNotFoundException(command.childId)

        require(child.status == ChildStatus.ACTIVE) {
            "Child ${child.id.value} must be ACTIVE to be assigned to a route"
        }

        val existingStops = stopRepository.findByRoute(command.companyId, command.routeId)

        val stopOrderExists = existingStops.any { it.stopOrder == command.stopOrder }
        if (stopOrderExists) {
            val stopsToUpdate = existingStops
                .filter { it.stopOrder >= command.stopOrder }
                .map { it.updateOrder(it.stopOrder + 1) }
            stopRepository.saveAll(stopsToUpdate)
        }

        val newStop = RouteStop.create(
            companyId = command.companyId,
            routeId = command.routeId,
            stopOrder = command.stopOrder,
            stopType = command.stopType,
            childId = command.childId,
            scheduleId = command.scheduleId,
            estimatedTime = command.estimatedTime,
            address = command.address
        )

        val savedStop = stopRepository.save(newStop)

        return AddRouteStopResult(
            id = savedStop.id,
            routeId = savedStop.routeId,
            stopOrder = savedStop.stopOrder,
            stopType = savedStop.stopType,
            childId = savedStop.childId
        )
    }
}