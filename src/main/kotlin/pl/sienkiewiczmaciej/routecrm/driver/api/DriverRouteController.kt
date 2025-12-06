package pl.sienkiewiczmaciej.routecrm.driver.api

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.api.dto.*
import pl.sienkiewiczmaciej.routecrm.driver.api.service.DriverRouteService
import pl.sienkiewiczmaciej.routecrm.driver.api.service.RouteChangeDetectionService
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.Instant

@RestController
@RequestMapping("/api/driver")
class DriverRouteController(
    private val driverRouteService: DriverRouteService,
    private val changeDetectionService: RouteChangeDetectionService
) : BaseController() {

    @GetMapping("/routes/today")
    suspend fun getUpcomingRoute(): ResponseEntity<DriverRouteDTO> {
        val principal = getPrincipal()
        val route = driverRouteService.getUpcomingRoute(principal)
        return ResponseEntity.ok(route)
    }

    @GetMapping("/routes/{routeId}")
    suspend fun getRouteDetail(
        @PathVariable routeId: String
    ): ResponseEntity<RouteDetailResponse> {
        val principal = getPrincipal()
        val detail = driverRouteService.getRouteDetail(principal, RouteId(routeId))
        return ResponseEntity.ok(detail)
    }

    @PostMapping("/routes/{routeId}/start")
    suspend fun startRoute(
        @PathVariable routeId: String
    ): ResponseEntity<DriverRouteDTO> {
        val principal = getPrincipal()
        val route = driverRouteService.startRoute(principal, RouteId(routeId))
        return ResponseEntity.ok(route)
    }

    @PostMapping("/stops/{stopId}/execute")
    suspend fun executeStop(
        @PathVariable stopId: String,
        @Valid @RequestBody request: ExecuteStopRequest
    ): ResponseEntity<ExecuteStopResponse> {
        val principal = getPrincipal()
        val response = driverRouteService.executeStop(principal, RouteStopId(stopId), request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/routes/{routeId}/changes")
    suspend fun getRouteChanges(
        @PathVariable routeId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) since: Instant
    ): ResponseEntity<DriverRouteChangesResponse> {
        val principal = getPrincipal()
        val changes = changeDetectionService.detectChanges(
            companyId = principal.companyId,
            routeId = RouteId(routeId),
            since = since
        )

        return if (changes.hasChanges) {
            ResponseEntity.ok(changes)
        } else {
            ResponseEntity.status(304).build()
        }
    }
}