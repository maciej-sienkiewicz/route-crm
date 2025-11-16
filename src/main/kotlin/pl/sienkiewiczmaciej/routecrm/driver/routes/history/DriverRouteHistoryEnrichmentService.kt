// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/history/DriverRouteHistoryEnrichmentService.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.temporal.ChronoUnit

@Component
class DriverRouteHistoryEnrichmentService(
    private val stopRepository: RouteStopRepository,
    private val vehicleRepository: VehicleJpaRepository
) {
    suspend fun enrichRoutes(
        routes: Page<Route>,
        companyId: CompanyId
    ): List<DriverRouteHistoryItem> = withContext(Dispatchers.IO) {
        routes.content.map { route ->
            async {
                enrichSingleRoute(route, companyId)
            }
        }.awaitAll()
    }

    private suspend fun enrichSingleRoute(
        route: Route,
        companyId: CompanyId
    ): DriverRouteHistoryItem {
        val vehicle = vehicleRepository.findByIdAndCompanyId(
            route.vehicleId.value,
            companyId.value
        )

        val stops = stopRepository.findByRoute(
            companyId = companyId,
            routeId = route.id,
            includeCancelled = true
        )

        val completedStops = stops.count { it.isExecuted() }
        val uniqueChildren = stops.map { it.childId }.toSet().size

        val wasPunctual = if (route.actualStartTime != null) {
            val estimatedStart = route.date.atTime(route.estimatedStartTime)
            val actualStart = route.actualStartTime.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            ChronoUnit.MINUTES.between(estimatedStart, actualStart) <= 5
        } else {
            true
        }

        val delayMinutes = if (route.actualStartTime != null) {
            val estimatedStart = route.date.atTime(route.estimatedStartTime)
            val actualStart = route.actualStartTime.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            val delay = ChronoUnit.MINUTES.between(estimatedStart, actualStart)
            if (delay > 5) delay.toInt() else null
        } else {
            null
        }

        return DriverRouteHistoryItem(
            id = route.id,
            routeName = route.routeName,
            date = route.date,
            status = route.status,
            vehicleId = route.vehicleId,
            vehicleRegistrationNumber = vehicle?.registrationNumber ?: "",
            estimatedStartTime = route.estimatedStartTime,
            estimatedEndTime = route.estimatedEndTime,
            actualStartTime = route.actualStartTime,
            actualEndTime = route.actualEndTime,
            stopsCount = stops.size,
            completedStopsCount = completedStops,
            childrenCount = uniqueChildren,
            wasPunctual = wasPunctual,
            delayMinutes = delayMinutes
        )
    }
}