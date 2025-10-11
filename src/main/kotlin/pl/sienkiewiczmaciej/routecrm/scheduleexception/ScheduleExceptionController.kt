package pl.sienkiewiczmaciej.routecrm.scheduleexception

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.create.CreateScheduleExceptionHandler
import pl.sienkiewiczmaciej.routecrm.scheduleexception.delete.DeleteScheduleExceptionCommand
import pl.sienkiewiczmaciej.routecrm.scheduleexception.delete.DeleteScheduleExceptionHandler
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.list.ListChildExceptionsHandler
import pl.sienkiewiczmaciej.routecrm.scheduleexception.list.ListChildExceptionsQuery
import pl.sienkiewiczmaciej.routecrm.scheduleexception.list.ListScheduleExceptionsHandler
import pl.sienkiewiczmaciej.routecrm.scheduleexception.list.ListScheduleExceptionsQuery
import pl.sienkiewiczmaciej.routecrm.scheduleexception.stats.GetChildExceptionStatsHandler
import pl.sienkiewiczmaciej.routecrm.scheduleexception.stats.GetChildExceptionStatsQuery
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class ScheduleExceptionController(
    private val createHandler: CreateScheduleExceptionHandler,
    private val listScheduleExceptionsHandler: ListScheduleExceptionsHandler,
    private val listChildExceptionsHandler: ListChildExceptionsHandler,
    private val deleteHandler: DeleteScheduleExceptionHandler,
    private val statsHandler: GetChildExceptionStatsHandler
) : BaseController() {

    @PostMapping("/schedules/{scheduleId}/exceptions")
    suspend fun create(
        @PathVariable scheduleId: String,
        @Valid @RequestBody request: CreateScheduleExceptionRequest
    ): ResponseEntity<ScheduleExceptionResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, ScheduleId.from(scheduleId))
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(ScheduleExceptionResponse.from(result))
    }

    @GetMapping("/schedules/{scheduleId}/exceptions")
    suspend fun listBySchedule(
        @PathVariable scheduleId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): ScheduleExceptionsListResponse {
        val principal = getPrincipal()
        val query = ListScheduleExceptionsQuery(
            companyId = principal.companyId,
            scheduleId = ScheduleId.from(scheduleId),
            from = from,
            to = to
        )
        val exceptions = listScheduleExceptionsHandler.handle(principal, query)
        return ScheduleExceptionsListResponse.from(exceptions)
    }

    @GetMapping("/children/{childId}/exceptions")
    suspend fun listByChild(
        @PathVariable childId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): ScheduleExceptionsListResponse {
        val principal = getPrincipal()
        val query = ListChildExceptionsQuery(
            companyId = principal.companyId,
            childId = ChildId.from(childId),
            from = from,
            to = to
        )
        val exceptions = listChildExceptionsHandler.handle(principal, query)
        return ScheduleExceptionsListResponse.from(exceptions)
    }

    @GetMapping("/children/{childId}/exception-stats")
    suspend fun getChildStats(
        @PathVariable childId: String,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?
    ): ChildExceptionStatsResponse {
        val principal = getPrincipal()
        val query = GetChildExceptionStatsQuery(
            companyId = principal.companyId,
            childId = ChildId.from(childId),
            year = year,
            month = month
        )
        val stats = statsHandler.handle(principal, query)
        return ChildExceptionStatsResponse.from(stats)
    }

    @DeleteMapping("/exceptions/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteScheduleExceptionCommand(
            companyId = principal.companyId,
            id = ScheduleExceptionId.from(id)
        )
        deleteHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }
}