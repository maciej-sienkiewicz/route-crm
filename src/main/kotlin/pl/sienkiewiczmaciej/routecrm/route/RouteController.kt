// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/RouteController.kt
package pl.sienkiewiczmaciej.routecrm.route

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.addschedule.AddRouteScheduleHandler
import pl.sienkiewiczmaciej.routecrm.route.availablechildren.ListAvailableChildrenHandler
import pl.sienkiewiczmaciej.routecrm.route.availablechildren.ListAvailableChildrenQuery
import pl.sienkiewiczmaciej.routecrm.route.cancelschedule.CancelRouteScheduleHandler
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.delete.DeleteRouteCommand
import pl.sienkiewiczmaciej.routecrm.route.delete.DeleteRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.deleteschedule.DeleteRouteScheduleCommand
import pl.sienkiewiczmaciej.routecrm.route.deleteschedule.DeleteRouteScheduleHandler
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.executestop.ExecuteRouteStopHandler
import pl.sienkiewiczmaciej.routecrm.route.getbyid.GetRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.getbyid.GetRouteQuery
import pl.sienkiewiczmaciej.routecrm.route.history.GetRouteHistoryHandler
import pl.sienkiewiczmaciej.routecrm.route.list.ListRoutesHandler
import pl.sienkiewiczmaciej.routecrm.route.list.ListRoutesQuery
import pl.sienkiewiczmaciej.routecrm.route.note.AddRouteNoteCommand
import pl.sienkiewiczmaciej.routecrm.route.note.AddRouteNoteHandler
import pl.sienkiewiczmaciej.routecrm.route.reorderstops.ReorderRouteStopsHandler
import pl.sienkiewiczmaciej.routecrm.route.suggestions.GetRouteSuggestionsHandler
import pl.sienkiewiczmaciej.routecrm.route.upcoming.GetUpcomingRoutesHandler
import pl.sienkiewiczmaciej.routecrm.route.updatestatus.UpdateRouteStatusCommand
import pl.sienkiewiczmaciej.routecrm.route.updatestatus.UpdateRouteStatusHandler
import pl.sienkiewiczmaciej.routecrm.route.updatestop.UpdateRouteStopHandler
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.LocalDate

@RestController
@RequestMapping("/api/routes")
class RouteController(
    private val createHandler: CreateRouteHandler,
    private val listHandler: ListRoutesHandler,
    private val getHandler: GetRouteHandler,
    private val updateStatusHandler: UpdateRouteStatusHandler,
    private val addScheduleHandler: AddRouteScheduleHandler,
    private val updateStopHandler: UpdateRouteStopHandler,
    private val reorderStopsHandler: ReorderRouteStopsHandler,
    private val deleteScheduleHandler: DeleteRouteScheduleHandler,
    private val cancelScheduleHandler: CancelRouteScheduleHandler,
    private val executeStopHandler: ExecuteRouteStopHandler,
    private val addNoteHandler: AddRouteNoteHandler,
    private val deleteHandler: DeleteRouteHandler,
    private val availableChildrenHandler: ListAvailableChildrenHandler,
    private val getRouteHistoryHandler: GetRouteHistoryHandler,
    private val getUpcomingRoutesHandler: GetUpcomingRoutesHandler,
    private val getRouteSuggestionsHandler: GetRouteSuggestionsHandler,
) : BaseController() {

    @GetMapping("/available-children")
    suspend fun getAvailableChildren(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): List<AvailableChildResponse> {
        val principal = getPrincipal()
        val query = ListAvailableChildrenQuery(
            companyId = principal.companyId,
            date = date
        )
        return availableChildrenHandler.handle(principal, query).map { item ->
            AvailableChildResponse.from(item, date)
        }
    }

    @PostMapping
    suspend fun create(
        @Valid @RequestBody request: CreateRouteRequest
    ): ResponseEntity<RouteResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId)
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(RouteResponse.from(result))
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
        @RequestParam(required = false) status: RouteStatus?,
        @RequestParam(required = false) driverId: String?,
        @PageableDefault(size = 20, sort = ["date", "estimatedStartTime"]) pageable: Pageable
    ): Page<RouteListResponse> {
        val principal = getPrincipal()
        val query = ListRoutesQuery(
            companyId = principal.companyId,
            date = date,
            status = status,
            driverId = driverId?.let { DriverId.from(it) },
            pageable = pageable
        )
        return listHandler.handle(principal, query).map { RouteListResponse.from(it) }
    }

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: String): RouteDetailResponse {
        val principal = getPrincipal()
        val query = GetRouteQuery(
            companyId = principal.companyId,
            id = RouteId.from(id)
        )
        return RouteDetailResponse.from(getHandler.handle(principal, query))
    }

    @PatchMapping("/{id}/status")
    suspend fun updateStatus(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateRouteStatusRequest
    ): UpdateRouteStatusResponse {
        val principal = getPrincipal()
        val command = UpdateRouteStatusCommand(
            companyId = principal.companyId,
            id = RouteId.from(id),
            status = request.status,
            actualStartTime = request.actualStartTime,
            actualEndTime = request.actualEndTime
        )
        val result = updateStatusHandler.handle(principal, command)
        return UpdateRouteStatusResponse.from(result)
    }

    /**
     * Dodaje schedule (pickup + dropoff) do trasy.
     * Endpoint: POST /api/routes/{routeId}/schedules
     */
    @PostMapping("/{routeId}/schedules")
    suspend fun addSchedule(
        @PathVariable routeId: String,
        @Valid @RequestBody request: AddRouteScheduleRequest
    ): ResponseEntity<AddRouteScheduleResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, RouteId.from(routeId))
        val result = addScheduleHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(AddRouteScheduleResponse.from(result))
    }

    /**
     * Aktualizuje pojedynczy stop (dla jednorazowych zmian adresu).
     * Endpoint: PATCH /api/routes/{routeId}/stops/{stopId}
     */
    @PatchMapping("/{routeId}/stops/{stopId}")
    suspend fun updateStop(
        @PathVariable routeId: String,
        @PathVariable stopId: String,
        @Valid @RequestBody request: UpdateRouteStopRequest
    ): UpdateRouteStopResponse {
        val principal = getPrincipal()
        val command = request.toCommand(
            principal.companyId,
            RouteId.from(routeId),
            RouteStopId.from(stopId)
        )
        val result = updateStopHandler.handle(principal, command)
        return UpdateRouteStopResponse.from(result)
    }

    /**
     * Zmienia kolejność stopów w trasie.
     * Endpoint: PATCH /api/routes/{routeId}/stops/reorder
     */
    @PatchMapping("/{routeId}/stops/reorder")
    suspend fun reorderStops(
        @PathVariable routeId: String,
        @Valid @RequestBody request: ReorderStopsRequest
    ): ReorderStopsResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, RouteId.from(routeId))
        val result = reorderStopsHandler.handle(principal, command)
        return ReorderStopsResponse.from(result)
    }

    /**
     * Usuwa schedule (pickup + dropoff) z trasy.
     * Endpoint: DELETE /api/routes/{routeId}/schedules/{scheduleId}
     */
    @DeleteMapping("/{routeId}/schedules/{scheduleId}")
    suspend fun deleteSchedule(
        @PathVariable routeId: String,
        @PathVariable scheduleId: String
    ): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteRouteScheduleCommand(
            companyId = principal.companyId,
            routeId = RouteId.from(routeId),
            scheduleId = ScheduleId.from(scheduleId)
        )
        deleteScheduleHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }

    /**
     * Anuluje schedule (pickup + dropoff) w trasie.
     * Endpoint: POST /api/routes/{routeId}/schedules/{scheduleId}/cancel
     */
    @PostMapping("/{routeId}/schedules/{scheduleId}/cancel")
    suspend fun cancelSchedule(
        @PathVariable routeId: String,
        @PathVariable scheduleId: String,
        @Valid @RequestBody request: CancelRouteScheduleRequest
    ): CancelRouteScheduleResponse {
        val principal = getPrincipal()
        val command = request.toCommand(
            principal.companyId,
            RouteId.from(routeId),
            ScheduleId.from(scheduleId)
        )
        val result = cancelScheduleHandler.handle(principal, command)
        return CancelRouteScheduleResponse.from(result)
    }

    /**
     * Wykonuje pojedynczy stop (pickup lub dropoff).
     * Endpoint: POST /api/routes/{routeId}/stops/{stopId}/execute
     */
    @PostMapping("/{routeId}/stops/{stopId}/execute")
    suspend fun executeStop(
        @PathVariable routeId: String,
        @PathVariable stopId: String,
        @Valid @RequestBody request: ExecuteRouteStopRequest
    ): ExecuteRouteStopResponse {
        val principal = getPrincipal()
        val command = request.toCommand(
            principal.companyId,
            RouteId.from(routeId),
            RouteStopId.from(stopId)
        )
        val result = executeStopHandler.handle(principal, command)
        return ExecuteRouteStopResponse.from(result)
    }

    @PostMapping("/{id}/notes")
    suspend fun addNote(
        @PathVariable id: String,
        @Valid @RequestBody request: AddRouteNoteRequest
    ): ResponseEntity<AddRouteNoteResponse> {
        val principal = getPrincipal()
        val command = AddRouteNoteCommand(
            companyId = principal.companyId,
            routeId = RouteId.from(id),
            content = request.content
        )
        val result = addNoteHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(AddRouteNoteResponse.from(result))
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteRouteCommand(
            companyId = principal.companyId,
            id = RouteId.from(id)
        )
        deleteHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }

    @GetMapping("/history")
    suspend fun getRouteHistory(
        @RequestParam scheduleId: String,
        @PageableDefault(size = 20, sort = ["date"], direction = org.springframework.data.domain.Sort.Direction.DESC) pageable: Pageable
    ): Page<RouteHistoryResponse> {
        val principal = getPrincipal()
        val query = pl.sienkiewiczmaciej.routecrm.route.history.GetRouteHistoryQuery(
            companyId = principal.companyId,
            scheduleId = ScheduleId.from(scheduleId),
            pageable = pageable
        )
        return getRouteHistoryHandler.handle(principal, query)
            .map { RouteHistoryResponse.from(it) }
    }

    @GetMapping("/upcoming")
    suspend fun getUpcomingRoutes(
        @RequestParam scheduleId: String,
        @PageableDefault(size = 20, sort = ["date", "estimatedStartTime"]) pageable: Pageable
    ): Page<UpcomingRouteResponse> {
        val principal = getPrincipal()
        val query = pl.sienkiewiczmaciej.routecrm.route.upcoming.GetUpcomingRoutesQuery(
            companyId = principal.companyId,
            scheduleId = ScheduleId.from(scheduleId),
            pageable = pageable
        )
        return getUpcomingRoutesHandler.handle(principal, query)
            .map { UpcomingRouteResponse.from(it) }
    }

    @GetMapping("/suggestions")
    suspend fun getRouteSuggestions(
        @RequestParam scheduleId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(defaultValue = "5") @Min(1) @Max(50) maxResults: Int
    ): List<RouteSuggestionResponse> {
        val principal = getPrincipal()
        val query = pl.sienkiewiczmaciej.routecrm.route.suggestions.GetRouteSuggestionsQuery(
            companyId = principal.companyId,
            scheduleId = ScheduleId.from(scheduleId),
            date = date,
            maxResults = maxResults
        )
        return getRouteSuggestionsHandler.handle(principal, query)
            .map { RouteSuggestionResponse.from(it) }
    }
}