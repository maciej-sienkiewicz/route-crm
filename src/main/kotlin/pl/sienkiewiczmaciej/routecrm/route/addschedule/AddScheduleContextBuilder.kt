package pl.sienkiewiczmaciej.routecrm.route.addschedule

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Vehicle
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleRepository

/**
 * Validation context for AddRouteSchedule operation.
 * Contains pre-loaded aggregates needed for validation.
 */
data class AddRouteScheduleValidationContext(
    val route: pl.sienkiewiczmaciej.routecrm.route.domain.Route,
    val child: Child,
    val schedule: Schedule,
    val vehicle: Vehicle,
    val existingStops: List<RouteStop>
)

@Component
class AddScheduleContextBuilder(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val childRepository: ChildRepository,
    private val scheduleRepository: ScheduleRepository,
    private val vehicleRepository: VehicleRepository
) {
    suspend fun build(command: AddRouteScheduleCommand): AddRouteScheduleValidationContext = coroutineScope {
        val routeDeferred = async {
            routeRepository.findById(command.companyId, command.routeId)
                ?: throw pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException(command.routeId)
        }

        val childDeferred = async {
            childRepository.findById(command.companyId, command.childId)
                ?: throw pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException(command.childId)
        }

        val scheduleDeferred = async {
            scheduleRepository.findById(command.companyId, command.scheduleId)
                ?: throw IllegalArgumentException("Schedule ${command.scheduleId.value} not found")
        }

        val stopsDeferred = async {
            stopRepository.findByRoute(command.companyId, command.routeId)
        }

        val route = routeDeferred.await()
        val child = childDeferred.await()
        val schedule = scheduleDeferred.await()
        val stops = stopsDeferred.await()

        val vehicle = vehicleRepository.findById(command.companyId, route.vehicleId)
            ?: throw pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.VehicleNotFoundException(route.vehicleId)

        return@coroutineScope AddRouteScheduleValidationContext(
            route = route,
            child = child,
            schedule = schedule,
            vehicle = vehicle,
            existingStops = stops
        )
    }
}