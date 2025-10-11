package pl.sienkiewiczmaciej.routecrm.schedule

import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.create.CreateScheduleHandler
import pl.sienkiewiczmaciej.routecrm.schedule.delete.DeleteScheduleCommand
import pl.sienkiewiczmaciej.routecrm.schedule.delete.DeleteScheduleHandler
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.GetScheduleHandler
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.GetScheduleQuery
import pl.sienkiewiczmaciej.routecrm.schedule.list.ListSchedulesHandler
import pl.sienkiewiczmaciej.routecrm.schedule.list.ListSchedulesQuery
import pl.sienkiewiczmaciej.routecrm.schedule.update.UpdateScheduleHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api")
class ScheduleController(
    private val createHandler: CreateScheduleHandler,
    private val listHandler: ListSchedulesHandler,
    private val getHandler: GetScheduleHandler,
    private val updateHandler: UpdateScheduleHandler,
    private val deleteHandler: DeleteScheduleHandler
) : BaseController() {

    @PostMapping("/children/{childId}/schedules")
    suspend fun create(
        @PathVariable childId: String,
        @Valid @RequestBody request: CreateScheduleRequest
    ): ResponseEntity<ScheduleResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, ChildId.from(childId))
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(
            ScheduleResponse.from(
                result,
                command.pickupAddress,
                command.dropoffAddress,
                command.specialInstructions
            )
        )
    }

    @GetMapping("/children/{childId}/schedules")
    suspend fun list(@PathVariable childId: String): SchedulesListResponse {
        val principal = getPrincipal()
        val query = ListSchedulesQuery(
            companyId = principal.companyId,
            childId = ChildId.from(childId)
        )
        val schedules = listHandler.handle(principal, query)
        return SchedulesListResponse.from(schedules)
    }

    @GetMapping("/schedules/{id}")
    suspend fun getById(@PathVariable id: String): ScheduleDetailResponse {
        val principal = getPrincipal()
        val query = GetScheduleQuery(
            companyId = principal.companyId,
            id = ScheduleId.from(id)
        )
        return ScheduleDetailResponse.from(getHandler.handle(principal, query))
    }

    @PutMapping("/schedules/{id}")
    suspend fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateScheduleRequest
    ): UpdateScheduleResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, ScheduleId.from(id))
        val result = updateHandler.handle(principal, command)
        return UpdateScheduleResponse.from(result)
    }

    @DeleteMapping("/schedules/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteScheduleCommand(
            companyId = principal.companyId,
            id = ScheduleId.from(id)
        )
        deleteHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }
}