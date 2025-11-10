package pl.sienkiewiczmaciej.routecrm.route.updatestop

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException

@Component
class UpdateRouteStopContextBuilder(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository
) {
    suspend fun build(command: UpdateRouteStopCommand): UpdateRouteStopValidationContext =
        coroutineScope {
            val routeDeferred = async {
                routeRepository.findById(command.companyId, command.routeId)
                    ?: throw RouteNotFoundException(command.routeId)
            }

            val stopDeferred = async {
                stopRepository.findById(command.companyId, command.stopId)
                    ?: throw RouteStopNotFoundException(command.stopId)
            }

            val route = routeDeferred.await()
            val stop = stopDeferred.await()

            return@coroutineScope UpdateRouteStopValidationContext(
                route = route,
                stop = stop
            )
    }
}