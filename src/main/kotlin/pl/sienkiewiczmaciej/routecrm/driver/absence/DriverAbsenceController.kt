// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/DriverAbsenceController.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.absence.cancel.CancelDriverAbsenceHandler
import pl.sienkiewiczmaciej.routecrm.driver.absence.create.CreateDriverAbsenceHandler
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import pl.sienkiewiczmaciej.routecrm.driver.absence.getbyid.GetDriverAbsenceHandler
import pl.sienkiewiczmaciej.routecrm.driver.absence.getbyid.GetDriverAbsenceQuery
import pl.sienkiewiczmaciej.routecrm.driver.absence.list.ListDriverAbsencesHandler
import pl.sienkiewiczmaciej.routecrm.driver.absence.list.ListDriverAbsencesQuery
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.LocalDate

@RestController
@RequestMapping("/api/drivers")
class DriverAbsenceController(
    private val createHandler: CreateDriverAbsenceHandler,
    private val listHandler: ListDriverAbsencesHandler,
    private val getHandler: GetDriverAbsenceHandler,
    private val cancelHandler: CancelDriverAbsenceHandler
) : BaseController() {

    @PostMapping("/{driverId}/absences")
    suspend fun create(
        @PathVariable driverId: String,
        @Valid @RequestBody request: CreateDriverAbsenceRequest
    ): ResponseEntity<DriverAbsenceResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, DriverId.from(driverId))
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(DriverAbsenceResponse.from(result))
    }

    @GetMapping("/{driverId}/absences")
    suspend fun listByDriver(
        @PathVariable driverId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) statuses: Set<DriverAbsenceStatus>?
    ): DriverAbsenceListResponse {
        val principal = getPrincipal()
        val query = ListDriverAbsencesQuery(
            companyId = principal.companyId,
            driverId = DriverId.from(driverId),
            from = from,
            to = to,
            statuses = statuses
        )
        val absences = listHandler.handle(principal, query)
        return DriverAbsenceListResponse.from(absences)
    }

    @GetMapping("/{driverId}/absences/{id}")
    suspend fun getById(
        @PathVariable driverId: String,
        @PathVariable id: String
    ): DriverAbsenceResponse {
        val principal = getPrincipal()
        val query = GetDriverAbsenceQuery(
            companyId = principal.companyId,
            id = DriverAbsenceId.from(id)
        )
        val absence = getHandler.handle(principal, query)
        return DriverAbsenceResponse.fromDetail(absence)
    }

    @PostMapping("/{driverId}/absences/{id}/cancel")
    suspend fun cancel(
        @PathVariable driverId: String,
        @PathVariable id: String,
        @Valid @RequestBody request: CancelDriverAbsenceRequest
    ): CancelDriverAbsenceResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, DriverAbsenceId.from(id))
        val result = cancelHandler.handle(principal, command)
        return CancelDriverAbsenceResponse.from(result)
    }
}