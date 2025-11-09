// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/RouteController.kt
package pl.sienkiewiczmaciej.routecrm.route

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.addstop.AddRouteStopHandler
import pl.sienkiewiczmaciej.routecrm.route.availablechildren.ListAvailableChildrenHandler
import pl.sienkiewiczmaciej.routecrm.route.availablechildren.ListAvailableChildrenQuery
import pl.sienkiewiczmaciej.routecrm.route.cancelstop.CancelRouteStopHandler
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.delete.DeleteRouteCommand
import pl.sienkiewiczmaciej.routecrm.route.delete.DeleteRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.deletestop.DeleteRouteStopCommand
import pl.sienkiewiczmaciej.routecrm.route.deletestop.DeleteRouteStopHandler
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.executestop.ExecuteRouteStopHandler
import pl.sienkiewiczmaciej.routecrm.route.getbyid.GetRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.getbyid.GetRouteQuery
import pl.sienkiewiczmaciej.routecrm.route.list.ListRoutesHandler
import pl.sienkiewiczmaciej.routecrm.route.list.ListRoutesQuery
import pl.sienkiewiczmaciej.routecrm.route.note.AddRouteNoteCommand
import pl.sienkiewiczmaciej.routecrm.route.note.AddRouteNoteHandler
import pl.sienkiewiczmaciej.routecrm.route.reorderstops.ReorderRouteStopsHandler
import pl.sienkiewiczmaciej.routecrm.route.updatestatus.UpdateRouteStatusCommand
import pl.sienkiewiczmaciej.routecrm.route.updatestatus.UpdateRouteStatusHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.LocalDate

@RestController
@RequestMapping("/api/routes")
class RouteController(
    private val createHandler: CreateRouteHandler,
    private val listHandler: ListRoutesHandler,
    private val getHandler: GetRouteHandler,
    private val updateStatusHandler: UpdateRouteStatusHandler,
    private val addStopHandler: AddRouteStopHandler,
    private val reorderStopsHandler: ReorderRouteStopsHandler,
    private val deleteStopHandler: DeleteRouteStopHandler,
    private val cancelStopHandler: CancelRouteStopHandler,
    private val executeStopHandler: ExecuteRouteStopHandler,
    private val addNoteHandler: AddRouteNoteHandler,
    private val deleteHandler: DeleteRouteHandler,
    private val availableChildrenHandler: ListAvailableChildrenHandler
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

    @PostMapping("/{routeId}/stops")
    suspend fun addStop(
        @PathVariable routeId: String,
        @Valid @RequestBody request: AddRouteStopRequest
    ): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, RouteId.from(routeId))
        addStopHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).build()
    }

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

    @DeleteMapping("/{routeId}/stops/{stopId}")
    suspend fun deleteStop(
        @PathVariable routeId: String,
        @PathVariable stopId: String
    ): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteRouteStopCommand(
            companyId = principal.companyId,
            routeId = RouteId.from(routeId),
            stopId = RouteStopId.from(stopId)
        )
        deleteStopHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }

    @PostMapping("/{routeId}/stops/{stopId}/cancel")
    suspend fun cancelStop(
        @PathVariable routeId: String,
        @PathVariable stopId: String,
        @Valid @RequestBody request: CancelRouteStopRequest
    ): CancelRouteStopResponse {
        val principal = getPrincipal()
        val command = request.toCommand(
            principal.companyId,
            RouteId.from(routeId),
            RouteStopId.from(stopId)
        )
        val result = cancelStopHandler.handle(principal, command)
        return CancelRouteStopResponse.from(result)
    }

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
}