// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/upcoming/UpcomingRoutesEnrichmentService.kt
package pl.sienkiewiczmaciej.routecrm.route.upcoming

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository

@Component
class UpcomingRoutesEnrichmentService(
    private val stopRepository: RouteStopRepository,
    private val driverRepository: DriverJpaRepository,
    private val vehicleRepository: VehicleJpaRepository,
    private val childRepository: ChildJpaRepository
) {
    suspend fun enrichRoutes(
        routes: Page<Route>,
        companyId: CompanyId,
        scheduleId: ScheduleId
    ): List<UpcomingRouteItem> = withContext(Dispatchers.IO) {
        routes.content.map { route ->
            async {
                enrichSingleRoute(route, companyId, scheduleId)
            }
        }.awaitAll()
    }

    private suspend fun enrichSingleRoute(
        route: Route,
        companyId: CompanyId,
        scheduleId: ScheduleId
    ): UpcomingRouteItem {
        val driver = driverRepository.findByIdAndCompanyId(
            route.driverId.value,
            companyId.value
        )

        val vehicle = vehicleRepository.findByIdAndCompanyId(
            route.vehicleId.value,
            companyId.value
        )

        // Get active stops for this schedule in this route
        val stops = stopRepository.findByRoute(
            companyId = companyId,
            routeId = route.id,
            includeCancelled = false
        ).filter { it.scheduleId == scheduleId }

        // Enrich stops with child details
        val childStops = withContext(Dispatchers.IO) {
            stops.map { stop ->
                async {
                    val child = childRepository.findByIdAndCompanyId(
                        stop.childId.value,
                        companyId.value
                    )

                    ChildStopDetail(
                        stopId = stop.id,
                        stopOrder = stop.stopOrder,
                        stopType = stop.stopType,
                        childFirstName = child?.firstName ?: "",
                        childLastName = child?.lastName ?: "",
                        estimatedTime = stop.estimatedTime,
                        address = stop.address
                    )
                }
            }.awaitAll()
        }.sortedBy { it.stopOrder }

        return UpcomingRouteItem(
            id = route.id,
            routeName = route.routeName,
            date = route.date,
            status = route.status,
            driverId = route.driverId,
            driverFirstName = driver?.firstName ?: "",
            driverLastName = driver?.lastName ?: "",
            vehicleId = route.vehicleId,
            vehicleRegistrationNumber = vehicle?.registrationNumber ?: "",
            vehicleModel = vehicle?.model ?: "",
            estimatedStartTime = route.estimatedStartTime,
            estimatedEndTime = route.estimatedEndTime,
            stopsCount = stops.size,
            childStops = childStops
        )
    }
}