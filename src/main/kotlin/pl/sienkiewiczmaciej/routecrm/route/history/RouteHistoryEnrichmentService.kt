// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/history/RouteHistoryEnrichmentService.kt
package pl.sienkiewiczmaciej.routecrm.route.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository

@Component
class RouteHistoryEnrichmentService(
    private val stopRepository: RouteStopRepository,
    private val driverRepository: DriverJpaRepository,
    private val vehicleRepository: VehicleJpaRepository
) {
    suspend fun enrichRoutes(
        routes: Page<Route>,
        companyId: CompanyId,
        scheduleId: ScheduleId
    ): List<RouteHistoryItem> = withContext(Dispatchers.IO) {
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
    ): RouteHistoryItem {
        val driver = route.driverId?.let { driverId ->
            driverRepository.findByIdAndCompanyId(
                driverId.value,
                companyId.value
            )
        }

        val vehicle = vehicleRepository.findByIdAndCompanyId(
            route.vehicleId.value,
            companyId.value
        )

        // Get stops for this schedule in this route
        val stops = stopRepository.findByRoute(
            companyId = companyId,
            routeId = route.id,
            includeCancelled = true
        ).filter { it.scheduleId == scheduleId }

        val completedStopsCount = stops.count { it.isExecuted() }

        return RouteHistoryItem(
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
            actualStartTime = route.actualStartTime,
            actualEndTime = route.actualEndTime,
            stopsCount = stops.size,
            completedStopsCount = completedStopsCount
        )
    }
}