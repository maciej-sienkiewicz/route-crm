// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/create/CreateRouteValidationContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.route.create

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.VehicleNotFoundException

@Component
class CreateRouteValidationContextBuilder(
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository,
    private val childRepository: ChildRepository,
    private val scheduleRepository: ScheduleRepository,
    private val routeRepository: RouteRepository
) {
    /**
     * Builds validation context by batch-loading all required data.
     * Uses coroutines to parallelize independent queries.
     */
    suspend fun build(command: CreateRouteCommand): CreateRouteValidationContext = coroutineScope {
        // Extract unique IDs
        val childrenIds = command.stops.map { it.childId }.toSet()
        val scheduleIds = command.stops.map { it.scheduleId }.toSet()

        // Parallel data loading
        val driverDeferred = async {
            command.driverId?.let { driverId ->
                driverRepository.findById(command.companyId, driverId)
                    ?: throw DriverNotFoundException(driverId)
            }
        }

        val vehicleDeferred = async {
            vehicleRepository.findById(command.companyId, command.vehicleId)
                ?: throw VehicleNotFoundException(command.vehicleId)
        }

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

        val existingDriverRoutesDeferred = async {
            command.driverId?.let { driverId ->
                routeRepository.findAll(
                    companyId = command.companyId,
                    date = command.date,
                    status = null,
                    driverId = driverId,
                    pageable = org.springframework.data.domain.Pageable.unpaged()
                ).content.filter { it.status in listOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS) }
            } ?: emptyList()
        }

        val existingVehicleRoutesDeferred = async {
            routeRepository.findAll(
                companyId = command.companyId,
                date = command.date,
                status = null,
                driverId = null,
                pageable = org.springframework.data.domain.Pageable.unpaged()
            ).content.filter {
                it.vehicleId == command.vehicleId &&
                        it.status in listOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS)
            }
        }

        // Await all results
        CreateRouteValidationContext(
            driver = driverDeferred.await(), // ← może być null
            vehicle = vehicleDeferred.await(),
            children = childrenDeferred.await(),
            schedules = schedulesDeferred.await(),
            existingDriverRoutes = existingDriverRoutesDeferred.await(),
            existingVehicleRoutes = existingVehicleRoutesDeferred.await()
        )
    }
}