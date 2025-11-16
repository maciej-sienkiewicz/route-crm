// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/DriverRoutesController.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.routes.history.GetDriverRouteHistoryHandler
import pl.sienkiewiczmaciej.routecrm.driver.routes.history.GetDriverRouteHistoryQuery
import pl.sienkiewiczmaciej.routecrm.driver.routes.upcoming.GetDriverUpcomingRoutesHandler
import pl.sienkiewiczmaciej.routecrm.driver.routes.upcoming.GetDriverUpcomingRoutesQuery
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/drivers")
class DriverRoutesController(
    private val routeHistoryHandler: GetDriverRouteHistoryHandler,
    private val upcomingRoutesHandler: GetDriverUpcomingRoutesHandler
) : BaseController() {

    @GetMapping("/{driverId}/routes/history")
    suspend fun getRouteHistory(
        @PathVariable driverId: String,
        @PageableDefault(size = 20, sort = ["date"], direction = org.springframework.data.domain.Sort.Direction.DESC) pageable: Pageable
    ): Page<DriverRouteHistoryResponse> {
        val principal = getPrincipal()
        val query = GetDriverRouteHistoryQuery(
            companyId = principal.companyId,
            driverId = DriverId.from(driverId),
            pageable = pageable
        )
        return routeHistoryHandler.handle(principal, query)
            .map { DriverRouteHistoryResponse.from(it) }
    }

    @GetMapping("/{driverId}/routes/upcoming")
    suspend fun getUpcomingRoutes(
        @PathVariable driverId: String,
        @PageableDefault(size = 20, sort = ["date", "estimatedStartTime"]) pageable: Pageable
    ): Page<DriverUpcomingRouteResponse> {
        val principal = getPrincipal()
        val query = GetDriverUpcomingRoutesQuery(
            companyId = principal.companyId,
            driverId = DriverId.from(driverId),
            pageable = pageable
        )
        return upcomingRoutesHandler.handle(principal, query)
            .map { DriverUpcomingRouteResponse.from(it) }
    }
}