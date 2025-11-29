// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/guardian/GuardianControllerExtensions.kt
package pl.sienkiewiczmaciej.routecrm.guardian

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryId
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactType
import pl.sienkiewiczmaciej.routecrm.guardian.contact.create.CreateContactHistoryCommand
import pl.sienkiewiczmaciej.routecrm.guardian.contact.create.CreateContactHistoryHandler
import pl.sienkiewiczmaciej.routecrm.guardian.contact.delete.DeleteContactHistoryCommand
import pl.sienkiewiczmaciej.routecrm.guardian.contact.delete.DeleteContactHistoryHandler
import pl.sienkiewiczmaciej.routecrm.guardian.contact.list.ListContactHistoryHandler
import pl.sienkiewiczmaciej.routecrm.guardian.contact.list.ListContactHistoryQuery
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteCategory
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteId
import pl.sienkiewiczmaciej.routecrm.guardian.note.create.CreateGuardianNoteCommand
import pl.sienkiewiczmaciej.routecrm.guardian.note.create.CreateGuardianNoteHandler
import pl.sienkiewiczmaciej.routecrm.guardian.note.delete.DeleteGuardianNoteCommand
import pl.sienkiewiczmaciej.routecrm.guardian.note.delete.DeleteGuardianNoteHandler
import pl.sienkiewiczmaciej.routecrm.guardian.note.list.ListGuardianNotesHandler
import pl.sienkiewiczmaciej.routecrm.guardian.note.list.ListGuardianNotesQuery
import pl.sienkiewiczmaciej.routecrm.guardian.note.update.UpdateGuardianNoteCommand
import pl.sienkiewiczmaciej.routecrm.guardian.note.update.UpdateGuardianNoteHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.Instant

data class CreateGuardianNoteRequest(
    @field:NotNull(message = "Category is required")
    val category: GuardianNoteCategory,

    @field:NotBlank(message = "Content is required")
    @field:Size(max = 5000)
    val content: String
) {
    fun toCommand(companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId, guardianId: GuardianId) =
        CreateGuardianNoteCommand(
            companyId = companyId,
            guardianId = guardianId,
            category = category,
            content = content
        )
}

data class GuardianNoteResponse(
    val id: String,
    val guardianId: String,
    val category: GuardianNoteCategory,
    val content: String,
    val createdAt: Instant,
    val createdByName: String,
    val createdById: String
) {
    companion object {
        fun from(result: pl.sienkiewiczmaciej.routecrm.guardian.note.create.CreateGuardianNoteResult) =
            GuardianNoteResponse(
                id = result.id.value,
                guardianId = result.guardianId.value,
                category = result.category,
                content = result.content,
                createdAt = result.createdAt,
                createdByName = result.createdByName,
                createdById = ""
            )

        fun from(item: pl.sienkiewiczmaciej.routecrm.guardian.note.list.GuardianNoteListItem) =
            GuardianNoteResponse(
                id = item.id.value,
                guardianId = item.guardianId.value,
                category = item.category,
                content = item.content,
                createdAt = item.createdAt,
                createdByName = item.createdByName,
                createdById = ""
            )

        fun from(result: pl.sienkiewiczmaciej.routecrm.guardian.note.update.UpdateGuardianNoteResult) =
            GuardianNoteResponse(
                id = result.id.value,
                guardianId = "",
                category = result.category,
                content = result.content,
                createdAt = result.createdAt,
                createdByName = result.createdByName,
                createdById = ""
            )
    }
}

data class UpdateGuardianNoteRequest(
    @field:NotBlank(message = "Category is required")
    val category: GuardianNoteCategory,

    @field:NotBlank(message = "Content is required")
    @field:Size(max = 5000)
    val content: String
) {
    fun toCommand(
        companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId,
        guardianId: GuardianId,
        noteId: GuardianNoteId
    ) = UpdateGuardianNoteCommand(
        companyId = companyId,
        guardianId = guardianId,
        noteId = noteId,
        category = category,
        content = content
    )
}

data class CreateContactHistoryRequest(
    @field:NotNull(message = "Type is required")
    val type: ContactType,

    @field:NotBlank(message = "Subject is required")
    @field:Size(max = 500)
    val subject: String,

    @field:NotBlank(message = "Notes are required")
    @field:Size(max = 5000)
    val notes: String
) {
    fun toCommand(companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId, guardianId: GuardianId) =
        CreateContactHistoryCommand(
            companyId = companyId,
            guardianId = guardianId,
            type = type,
            subject = subject,
            notes = notes
        )
}

data class ContactHistoryResponse(
    val id: String,
    val guardianId: String,
    val type: ContactType,
    val subject: String,
    val notes: String,
    val contactedAt: Instant,
    val handledByName: String
) {
    companion object {
        fun from(result: pl.sienkiewiczmaciej.routecrm.guardian.contact.create.CreateContactHistoryResult) =
            ContactHistoryResponse(
                id = result.id.value,
                guardianId = "",
                type = result.type,
                subject = result.subject,
                notes = result.notes,
                contactedAt = result.contactedAt,
                handledByName = result.handledByName
            )

        fun from(item: pl.sienkiewiczmaciej.routecrm.guardian.contact.list.ContactHistoryListItem) =
            ContactHistoryResponse(
                id = item.id.value,
                guardianId = "",
                type = item.type,
                subject = item.subject,
                notes = item.notes,
                contactedAt = item.contactedAt,
                handledByName = item.handledByName
            )
    }
}

@RestController
@RequestMapping("/api/guardians")
class GuardianNotesController(
    private val listNotesHandler: ListGuardianNotesHandler,
    private val createNoteHandler: CreateGuardianNoteHandler,
    private val updateNoteHandler: UpdateGuardianNoteHandler,
    private val deleteNoteHandler: DeleteGuardianNoteHandler
) : BaseController() {

    @GetMapping("/{guardianId}/notes")
    suspend fun getNotes(@PathVariable guardianId: String): List<GuardianNoteResponse> {
        val principal = getPrincipal()
        val query = ListGuardianNotesQuery(
            companyId = principal.companyId,
            guardianId = GuardianId.from(guardianId)
        )
        return listNotesHandler.handle(principal, query).map { GuardianNoteResponse.from(it) }
    }

    @PostMapping("/{guardianId}/notes")
    suspend fun createNote(
        @PathVariable guardianId: String,
        @Valid @RequestBody request: CreateGuardianNoteRequest
    ): ResponseEntity<GuardianNoteResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, GuardianId.from(guardianId))
        val result = createNoteHandler.handle(principal, command)
        return ResponseEntity.status(HttpStatus.CREATED).body(GuardianNoteResponse.from(result))
    }

    @PutMapping("/{guardianId}/notes/{noteId}")
    suspend fun updateNote(
        @PathVariable guardianId: String,
        @PathVariable noteId: String,
        @Valid @RequestBody request: UpdateGuardianNoteRequest
    ): GuardianNoteResponse {
        val principal = getPrincipal()
        val command = request.toCommand(
            principal.companyId,
            GuardianId.from(guardianId),
            GuardianNoteId.from(noteId)
        )
        val result = updateNoteHandler.handle(principal, command)
        return GuardianNoteResponse.from(result)
    }

    @DeleteMapping("/{guardianId}/notes/{noteId}")
    suspend fun deleteNote(
        @PathVariable guardianId: String,
        @PathVariable noteId: String
    ): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteGuardianNoteCommand(
            companyId = principal.companyId,
            guardianId = GuardianId.from(guardianId),
            noteId = GuardianNoteId.from(noteId)
        )
        deleteNoteHandler.handle(principal, command)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}

@RestController
@RequestMapping("/api/guardians")
class GuardianContactHistoryController(
    private val listContactsHandler: ListContactHistoryHandler,
    private val createContactHandler: CreateContactHistoryHandler,
    private val deleteContactHandler: DeleteContactHistoryHandler
) : BaseController() {

    @GetMapping("/{guardianId}/contacts")
    suspend fun getContactHistory(@PathVariable guardianId: String): List<ContactHistoryResponse> {
        val principal = getPrincipal()
        val query = ListContactHistoryQuery(
            companyId = principal.companyId,
            guardianId = GuardianId.from(guardianId)
        )
        return listContactsHandler.handle(principal, query).map { ContactHistoryResponse.from(it) }
    }

    @PostMapping("/{guardianId}/contacts")
    suspend fun createContact(
        @PathVariable guardianId: String,
        @Valid @RequestBody request: CreateContactHistoryRequest
    ): ResponseEntity<ContactHistoryResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, GuardianId.from(guardianId))
        val result = createContactHandler.handle(principal, command)
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactHistoryResponse.from(result))
    }

    @DeleteMapping("/{guardianId}/contacts/{contactId}")
    suspend fun deleteContact(
        @PathVariable guardianId: String,
        @PathVariable contactId: String
    ): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteContactHistoryCommand(
            companyId = principal.companyId,
            guardianId = GuardianId.from(guardianId),
            contactId = ContactHistoryId.from(contactId)
        )
        deleteContactHandler.handle(principal, command)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}