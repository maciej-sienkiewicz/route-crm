// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/DriverNoteController.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes

import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.notes.create.CreateDriverNoteHandler
import pl.sienkiewiczmaciej.routecrm.driver.notes.delete.DeleteDriverNoteCommand
import pl.sienkiewiczmaciej.routecrm.driver.notes.delete.DeleteDriverNoteHandler
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.list.ListDriverNotesHandler
import pl.sienkiewiczmaciej.routecrm.driver.notes.list.ListDriverNotesQuery
import pl.sienkiewiczmaciej.routecrm.driver.notes.update.UpdateDriverNoteHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/drivers")
class DriverNoteController(
    private val createHandler: CreateDriverNoteHandler,
    private val listHandler: ListDriverNotesHandler,
    private val updateHandler: UpdateDriverNoteHandler,
    private val deleteHandler: DeleteDriverNoteHandler
) : BaseController() {

    @PostMapping("/{driverId}/notes")
    suspend fun create(
        @PathVariable driverId: String,
        @Valid @RequestBody request: CreateDriverNoteRequest
    ): ResponseEntity<DriverNoteResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, DriverId.from(driverId))
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(DriverNoteResponse.from(result))
    }

    @GetMapping("/{driverId}/notes")
    suspend fun list(@PathVariable driverId: String): DriverNoteListResponse {
        val principal = getPrincipal()
        val query = ListDriverNotesQuery(
            companyId = principal.companyId,
            driverId = DriverId.from(driverId)
        )
        val notes = listHandler.handle(principal, query)
        return DriverNoteListResponse.from(notes)
    }

    @PutMapping("/{driverId}/notes/{noteId}")
    suspend fun update(
        @PathVariable driverId: String,
        @PathVariable noteId: String,
        @Valid @RequestBody request: UpdateDriverNoteRequest
    ): DriverNoteResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, DriverNoteId.from(noteId))
        val result = updateHandler.handle(principal, command)
        return DriverNoteResponse.fromUpdate(result)
    }

    @DeleteMapping("/{driverId}/notes/{noteId}")
    suspend fun delete(
        @PathVariable driverId: String,
        @PathVariable noteId: String
    ): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteDriverNoteCommand(
            companyId = principal.companyId,
            id = DriverNoteId.from(noteId)
        )
        deleteHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }
}