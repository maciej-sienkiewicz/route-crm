// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/history/GetDriverRouteHistoryContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes.history

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus

data class GetDriverRouteHistoryValidationContext(
    val driver: Driver,
    val routes: Page<Route>
)

@Component
class GetDriverRouteHistoryContextBuilder(
    private val driverRepository: DriverRepository,
    private val routeRepository: RouteRepository
) {
    suspend fun build(query: GetDriverRouteHistoryQuery): GetDriverRouteHistoryValidationContext = coroutineScope {
        val driverDeferred = async {
            driverRepository.findById(query.companyId, query.driverId)
                ?: throw DriverNotFoundException(query.driverId)
        }

        val routesDeferred = async {
            routeRepository.findByDriver(
                companyId = query.companyId,
                driverId = query.driverId,
                date = null,
                pageable = query.pageable
            )
        }

        val routes = routesDeferred.await()
        val completedRoutes = routes.content.filter {
            it.status in setOf(RouteStatus.COMPLETED, RouteStatus.CANCELLED)
        }

        GetDriverRouteHistoryValidationContext(
            driver = driverDeferred.await(),
            routes = org.springframework.data.domain.PageImpl(
                completedRoutes,
                routes.pageable,
                routes.totalElements
            )
        )
    }
}