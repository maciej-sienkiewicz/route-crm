// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/cancelschedule/CancelRouteScheduleHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.cancelschedule

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
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
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CancelRouteScheduleCommand): CancelRouteScheduleResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        // Znajdź wszystkie stopy dla tego schedule
        val allStops = stopRepository.findByRoute(command.companyId, command.routeId, includeCancelled = false)
        val scheduleStops = allStops.filter { it.scheduleId == command.scheduleId }

        require(scheduleStops.isNotEmpty()) {
            "Schedule ${command.scheduleId.value} not found in route ${command.routeId.value}"
        }

        // Powinny być dokładnie 2 stopy: pickup i dropoff
        require(scheduleStops.size == 2) {
            "Expected 2 stops (pickup and dropoff) for schedule ${command.scheduleId.value}, found ${scheduleStops.size}"
        }

        val pickupStop = scheduleStops.find { it.stopType == pl.sienkiewiczmaciej.routecrm.route.domain.StopType.PICKUP }
            ?: throw IllegalStateException("Pickup stop not found for schedule ${command.scheduleId.value}")

        val dropoffStop = scheduleStops.find { it.stopType == pl.sienkiewiczmaciej.routecrm.route.domain.StopType.DROPOFF }
            ?: throw IllegalStateException("Dropoff stop not found for schedule ${command.scheduleId.value}")

        // Waliduj że stopy mogą być anulowane
        require(!pickupStop.isExecuted() && !dropoffStop.isExecuted()) {
            "Cannot cancel schedule: one or both stops have already been executed"
        }

        require(!pickupStop.isCancelled && !dropoffStop.isCancelled) {
            "Schedule ${command.scheduleId.value} is already cancelled"
        }

        // Anuluj oba stopy
        val cancelledPickup = pickupStop.cancel(command.reason)
        val cancelledDropoff = dropoffStop.cancel(command.reason)

        stopRepository.save(cancelledPickup)
        stopRepository.save(cancelledDropoff)

        return CancelRouteScheduleResult(
            scheduleId = command.scheduleId,
            pickupStopId = cancelledPickup.id,
            dropoffStopId = cancelledDropoff.id,
            cancelledStopsCount = 2,
            cancelledAt = Instant.now()
        )
    }
}