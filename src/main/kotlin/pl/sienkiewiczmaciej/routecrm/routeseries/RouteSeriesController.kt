// routeseries/RouteSeriesController.kt
package pl.sienkiewiczmaciej.routecrm.routeseries

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.routeseries.addchild.AddChildToRouteSeriesHandler
import pl.sienkiewiczmaciej.routecrm.routeseries.cancel.CancelRouteSeriesHandler
import pl.sienkiewiczmaciej.routecrm.routeseries.create.CreateRouteSeriesFromRouteHandler
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.getbyid.GetRouteSeriesHandler
import pl.sienkiewiczmaciej.routecrm.routeseries.getbyid.GetRouteSeriesQuery
import pl.sienkiewiczmaciej.routecrm.routeseries.list.ListRouteSeriesHandler
import pl.sienkiewiczmaciej.routecrm.routeseries.list.ListRouteSeriesQuery
import pl.sienkiewiczmaciej.routecrm.routeseries.removechild.RemoveChildFromRouteSeriesHandler
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/route-series")
class RouteSeriesController(
    private val createFromRouteHandler: CreateRouteSeriesFromRouteHandler,
    private val listHandler: ListRouteSeriesHandler,
    private val getHandler: GetRouteSeriesHandler,
    private val addChildHandler: AddChildToRouteSeriesHandler,
    private val removeChildHandler: RemoveChildFromRouteSeriesHandler,
    private val cancelHandler: CancelRouteSeriesHandler
) : BaseController() {

    @PostMapping("/from-route/{routeId}")
    suspend fun createFromRoute(
        @PathVariable routeId: String,
        @Valid @RequestBody request: CreateRouteSeriesFromRouteRequest
    ): ResponseEntity<CreateRouteSeriesFromRouteResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, RouteId.from(routeId))
        val result = createFromRouteHandler.handle(principal, command)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CreateRouteSeriesFromRouteResponse.from(result))
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false) status: RouteSeriesStatus?,
        @PageableDefault(size = 20, sort = ["createdAt"]) pageable: Pageable
    ): Page<RouteSeriesListResponse> {
        val principal = getPrincipal()
        val query = ListRouteSeriesQuery(
            companyId = principal.companyId,
            status = status,
            pageable = pageable
        )

        return listHandler.handle(principal, query)
    }

    @GetMapping("/{seriesId}")
    suspend fun getById(@PathVariable seriesId: String): RouteSeriesResponse {
        val principal = getPrincipal()
        val query = GetRouteSeriesQuery(
            companyId = principal.companyId,
            seriesId = RouteSeriesId.from(seriesId)
        )

        val series = getHandler.handle(principal, query)
        return RouteSeriesResponse.from(series)
    }

    @PostMapping("/{seriesId}/schedules")
    suspend fun addChild(
        @PathVariable seriesId: String,
        @Valid @RequestBody request: AddChildToSeriesRequest
    ): ResponseEntity<AddChildToSeriesResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, RouteSeriesId.from(seriesId))
        val result = addChildHandler.handle(principal, command)

        val message = if (result.conflictResolved) {
            "Changes detected from ${result.effectiveTo!!.plusDays(1)}. " +
                    "Changes applied from ${result.effectiveFrom} to ${result.effectiveTo}."
        } else null

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AddChildToSeriesResponse.from(result, message))
    }

    @DeleteMapping("/{seriesId}/schedules/{scheduleId}")
    suspend fun removeChild(
        @PathVariable seriesId: String,
        @PathVariable scheduleId: String,
        @Valid @RequestBody request: RemoveChildFromSeriesRequest
    ): ResponseEntity<RemoveChildFromSeriesResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(
            principal.companyId,
            RouteSeriesId.from(seriesId),
            ScheduleId.from(scheduleId)
        )

        val result = removeChildHandler.handle(principal, command)
        return ResponseEntity.ok(RemoveChildFromSeriesResponse.from(result))
    }

    @PostMapping("/{seriesId}/cancel")
    suspend fun cancel(
        @PathVariable seriesId: String,
        @Valid @RequestBody request: CancelRouteSeriesRequest
    ): ResponseEntity<CancelRouteSeriesResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, RouteSeriesId.from(seriesId))
        val result = cancelHandler.handle(principal, command)

        return ResponseEntity.ok(CancelRouteSeriesResponse.from(result))
    }
}