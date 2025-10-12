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
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.delete.DeleteRouteCommand
import pl.sienkiewiczmaciej.routecrm.route.delete.DeleteRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.getbyid.GetRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.getbyid.GetRouteQuery
import pl.sienkiewiczmaciej.routecrm.route.list.ListRoutesHandler
import pl.sienkiewiczmaciej.routecrm.route.list.ListRoutesQuery
import pl.sienkiewiczmaciej.routecrm.route.note.AddRouteNoteCommand
import pl.sienkiewiczmaciej.routecrm.route.note.AddRouteNoteHandler
import pl.sienkiewiczmaciej.routecrm.route.updatechildstatus.UpdateChildStatusCommand
import pl.sienkiewiczmaciej.routecrm.route.updatechildstatus.UpdateChildStatusHandler
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
    private val updateChildStatusHandler: UpdateChildStatusHandler,
    private val addNoteHandler: AddRouteNoteHandler,
    private val deleteHandler: DeleteRouteHandler
) : BaseController() {

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

    @PatchMapping("/{routeId}/children/{childId}/status")
    suspend fun updateChildStatus(
        @PathVariable routeId: String,
        @PathVariable childId: String,
        @Valid @RequestBody request: UpdateChildStatusRequest
    ): UpdateChildStatusResponse {
        val principal = getPrincipal()
        val command = UpdateChildStatusCommand(
            companyId = principal.companyId,
            routeId = RouteId.from(routeId),
            childId = ChildId.from(childId),
            status = request.status,
            actualPickupTime = request.actualPickupTime,
            actualDropoffTime = request.actualDropoffTime
        )
        val result = updateChildStatusHandler.handle(principal, command)
        return UpdateChildStatusResponse.from(result)
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