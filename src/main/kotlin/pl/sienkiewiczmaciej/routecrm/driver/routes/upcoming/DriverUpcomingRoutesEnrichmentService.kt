// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/upcoming/DriverUpcomingRoutesEnrichmentService.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes.upcoming

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

@Component
class DriverUpcomingRoutesEnrichmentService(
    private val stopRepository: RouteStopRepository,
    private val vehicleRepository: VehicleJpaRepository
) {
    suspend fun enrichRoutes(
        routes: Page<Route>,
        companyId: CompanyId
    ): List<DriverUpcomingRouteItem> = withContext(Dispatchers.IO) {
        routes.content.map { route ->
            async {
                enrichSingleRoute(route, companyId)
            }
        }.awaitAll()
    }

    private suspend fun enrichSingleRoute(
        route: Route,
        companyId: CompanyId
    ): DriverUpcomingRouteItem {
        val vehicle = vehicleRepository.findByIdAndCompanyId(
            route.vehicleId.value,
            companyId.value
        )

        val stops = stopRepository.findByRoute(
            companyId = companyId,
            routeId = route.id,
            includeCancelled = false
        ).sortedBy { it.stopOrder }

        val uniqueChildren = stops.map { it.childId }.toSet().size

        val firstStopAddress = stops.firstOrNull()?.let {
            "${it.address.address.street} ${it.address.address.houseNumber}, ${it.address.address.city}"
        } ?: ""

        val lastStopAddress = stops.lastOrNull()?.let {
            "${it.address.address.street} ${it.address.address.houseNumber}, ${it.address.address.city}"
        } ?: ""

        return DriverUpcomingRouteItem(
            id = route.id,
            routeName = route.routeName,
            date = route.date,
            status = route.status,
            vehicleId = route.vehicleId,
            vehicleRegistrationNumber = vehicle?.registrationNumber ?: "",
            estimatedStartTime = route.estimatedStartTime,
            estimatedEndTime = route.estimatedEndTime,
            stopsCount = stops.size,
            childrenCount = uniqueChildren,
            firstStopAddress = firstStopAddress,
            lastStopAddress = lastStopAddress
        )
    }
}