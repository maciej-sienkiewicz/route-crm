// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/cancelschedule/CancelScheduleValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.cancelschedule

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException

/**
 * Validation context for CancelRouteSchedule operation.
 */
data class CancelScheduleValidationContext(
    val route: Route,
    val scheduleStops: List<RouteStop>
)
@Component
class CancelScheduleContextBuilder(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository
) {
    suspend fun build(command: CancelRouteScheduleCommand): CancelScheduleValidationContext = coroutineScope {
            val routeDeferred = async {
                routeRepository.findById(command.companyId, command.routeId)
                    ?: throw RouteNotFoundException(command.routeId)
            }

            val stopsDeferred = async {
                stopRepository.findByRoute(command.companyId, command.routeId, includeCancelled = false)
            }

            val route = routeDeferred.await()
            val allStops = stopsDeferred.await()
            val scheduleStops = allStops.filter { it.scheduleId == command.scheduleId }

            return@coroutineScope CancelScheduleValidationContext(
                route = route,
                scheduleStops = scheduleStops
            )
    }
}