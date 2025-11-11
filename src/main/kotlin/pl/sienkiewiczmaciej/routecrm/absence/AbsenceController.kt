// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/AbsenceController.kt
package pl.sienkiewiczmaciej.routecrm.absence

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.absence.cancel.CancelAbsenceHandler
import pl.sienkiewiczmaciej.routecrm.absence.create.CreateAbsenceHandler
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.absence.getbyid.GetAbsenceHandler
import pl.sienkiewiczmaciej.routecrm.absence.getbyid.GetAbsenceQuery
import pl.sienkiewiczmaciej.routecrm.absence.list.ListAbsencesByChildHandler
import pl.sienkiewiczmaciej.routecrm.absence.list.ListAbsencesByChildQuery
import pl.sienkiewiczmaciej.routecrm.absence.list.ListAbsencesByScheduleHandler
import pl.sienkiewiczmaciej.routecrm.absence.list.ListAbsencesByScheduleQuery
import pl.sienkiewiczmaciej.routecrm.absence.stats.GetAbsenceStatsHandler
import pl.sienkiewiczmaciej.routecrm.absence.stats.GetAbsenceStatsQuery
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
class AbsenceController(
    private val createHandler: CreateAbsenceHandler,
    private val listByChildHandler: ListAbsencesByChildHandler,
    private val listByScheduleHandler: ListAbsencesByScheduleHandler,
    private val getHandler: GetAbsenceHandler,
    private val cancelHandler: CancelAbsenceHandler,
    private val statsHandler: GetAbsenceStatsHandler
) : BaseController() {

    @PostMapping("/children/{childId}/absences")
    suspend fun create(
        @PathVariable childId: String,
        @Valid @RequestBody request: CreateAbsenceRequest
    ): ResponseEntity<AbsenceResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, ChildId.from(childId))
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(AbsenceResponse.from(result))
    }

    @GetMapping("/children/{childId}/absences")
    suspend fun listByChild(
        @PathVariable childId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) statuses: Set<AbsenceStatus>?
    ): AbsenceListResponse {
        val principal = getPrincipal()
        val query = ListAbsencesByChildQuery(
            companyId = principal.companyId,
            childId = ChildId.from(childId),
            from = from,
            to = to,
            statuses = statuses
        )
        val absences = listByChildHandler.handle(principal, query)
        return AbsenceListResponse.from(absences)
    }

    @GetMapping("/schedules/{scheduleId}/absences")
    suspend fun listBySchedule(
        @PathVariable scheduleId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): AbsenceListResponse {
        val principal = getPrincipal()
        val query = ListAbsencesByScheduleQuery(
            companyId = principal.companyId,
            scheduleId = ScheduleId.from(scheduleId),
            from = from,
            to = to
        )
        val absences = listByScheduleHandler.handle(principal, query)
        return AbsenceListResponse.from(absences)
    }

    @GetMapping("/absences/{id}")
    suspend fun getById(@PathVariable id: String): AbsenceResponse {
        val principal = getPrincipal()
        val query = GetAbsenceQuery(
            companyId = principal.companyId,
            id = ChildAbsenceId.from(id)
        )
        val absence = getHandler.handle(principal, query)
        return AbsenceResponse.from(absence)
    }

    @PostMapping("/absences/{id}/cancel")
    suspend fun cancel(
        @PathVariable id: String,
        @Valid @RequestBody request: CancelAbsenceRequest
    ): CancelAbsenceResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, ChildAbsenceId.from(id))
        val result = cancelHandler.handle(principal, command)
        return CancelAbsenceResponse.from(result)
    }

    @GetMapping("/children/{childId}/absence-stats")
    suspend fun getStats(
        @PathVariable childId: String,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?
    ): AbsenceStatsResponse {
        val principal = getPrincipal()
        val query = GetAbsenceStatsQuery(
            companyId = principal.companyId,
            childId = ChildId.from(childId),
            year = year,
            month = month
        )
        val stats = statsHandler.handle(principal, query)
        return AbsenceStatsResponse.from(stats)
    }
}