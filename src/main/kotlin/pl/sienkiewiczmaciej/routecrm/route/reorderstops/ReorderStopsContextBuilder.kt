package pl.sienkiewiczmaciej.routecrm.route.reorderstops

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException

data class ReorderStopsValidationContext(
    val route: Route,
    val existingStops: List<RouteStop>
)

@Component
class ReorderStopsContextBuilder(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository
) {
    suspend fun build(command: ReorderRouteStopsCommand): ReorderStopsValidationContext = coroutineScope {
            val routeDeferred = async {
                routeRepository.findById(command.companyId, command.routeId)
                    ?: throw RouteNotFoundException(command.routeId)
            }

            val stopsDeferred = async {
                stopRepository.findByRoute(command.companyId, command.routeId)
            }

            val route = routeDeferred.await()
            val stops = stopsDeferred.await()

            return@coroutineScope ReorderStopsValidationContext(
                route = route,
                existingStops = stops
            )
        }
}