// routeseries/create/CreateRouteSeriesValidationContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.create

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.VehicleNotFoundException

@Component
class CreateRouteSeriesValidationContextBuilder(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository,
    private val childRepository: ChildRepository,
    private val scheduleRepository: ScheduleRepository
) {
    suspend fun build(command: CreateRouteSeriesFromRouteCommand): CreateRouteSeriesValidationContext = coroutineScope {
        val routeDeferred = async {
            routeRepository.findById(command.companyId, command.sourceRouteId)
                ?: throw RouteNotFoundException(command.sourceRouteId)
        }

        val route = routeDeferred.await()

        val stopsDeferred = async {
            stopRepository.findByRoute(
                companyId = command.companyId,
                routeId = command.sourceRouteId,
                includeCancelled = false
            )
        }

        val driverDeferred = async {
            route.driverId?.let { driverId ->
                driverRepository.findById(command.companyId, route.driverId)
                    ?: throw DriverNotFoundException(route.driverId)
            }
        }

        val vehicleDeferred = async {
            vehicleRepository.findById(command.companyId, route.vehicleId)
                ?: throw VehicleNotFoundException(route.vehicleId)
        }

        val stops = stopsDeferred.await()
        val childrenIds = stops.map { it.childId }.toSet()
        val scheduleIds = stops.map { it.scheduleId }.toSet()

        val childrenDeferred = async {
            childrenIds.associateWith { childId ->
                childRepository.findById(command.companyId, childId)
                    ?: throw ChildNotFoundException(childId)
            }
        }

        val schedulesDeferred = async {
            scheduleIds.associateWith { scheduleId ->
                scheduleRepository.findById(command.companyId, scheduleId)
                    ?: throw IllegalArgumentException("Schedule ${scheduleId.value} not found")
            }
        }

        CreateRouteSeriesValidationContext(
            sourceRoute = route,
            sourceRouteStops = stops,
            driver = driverDeferred.await(),
            vehicle = vehicleDeferred.await(),
            children = childrenDeferred.await(),
            schedules = schedulesDeferred.await()
        )
    }
}