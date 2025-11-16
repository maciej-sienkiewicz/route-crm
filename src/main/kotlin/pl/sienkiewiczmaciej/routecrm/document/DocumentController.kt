// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/DocumentController.kt
package pl.sienkiewiczmaciej.routecrm.document

import jakarta.validation.Valid
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.document.confirm.ConfirmUploadHandler
import pl.sienkiewiczmaciej.routecrm.document.delete.DeleteDocumentCommand
import pl.sienkiewiczmaciej.routecrm.document.delete.DeleteDocumentHandler
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.document.getbyid.GetDocumentHandler
import pl.sienkiewiczmaciej.routecrm.document.getbyid.GetDocumentQuery
import pl.sienkiewiczmaciej.routecrm.document.list.ListDocumentsHandler
import pl.sienkiewiczmaciej.routecrm.document.list.ListDocumentsQuery
import pl.sienkiewiczmaciej.routecrm.document.uploadurl.GetUploadUrlHandler
import pl.sienkiewiczmaciej.routecrm.document.viewurl.GetViewUrlCommand
import pl.sienkiewiczmaciej.routecrm.document.viewurl.GetViewUrlHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/documents")
class DocumentController(
    private val getUploadUrlHandler: GetUploadUrlHandler,
    private val confirmUploadHandler: ConfirmUploadHandler,
    private val getViewUrlHandler: GetViewUrlHandler,
    private val listDocumentsHandler: ListDocumentsHandler,
    private val getDocumentHandler: GetDocumentHandler,
    private val deleteDocumentHandler: DeleteDocumentHandler
) : BaseController() {

    @PostMapping("/upload-url")
    suspend fun getUploadUrl(
        @Valid @RequestBody request: GetUploadUrlRequest
    ): ResponseEntity<UploadUrlResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId)
        val result = getUploadUrlHandler.handle(principal, command)
        return ResponseEntity.ok(UploadUrlResponse.from(result))
    }

    @GetMapping("/{id}/view-url")
    suspend fun getViewUrl(@PathVariable id: String): ViewUrlResponse {
        val principal = getPrincipal()
        val command = GetViewUrlCommand(
            companyId = principal.companyId,
            documentId = DocumentId.from(id)
        )
        val result = getViewUrlHandler.handle(principal, command)
        return ViewUrlResponse.from(result)
    }

    @GetMapping
    suspend fun list(
        @RequestParam entityType: EntityType,
        @RequestParam entityId: String,
        @RequestParam(required = false) documentType: DocumentType?
    ): DocumentListResponse {
        val principal = getPrincipal()
        val query = ListDocumentsQuery(
            companyId = principal.companyId,
            entityType = entityType,
            entityId = entityId,
            documentType = documentType
        )
        val documents = listDocumentsHandler.handle(principal, query)
        return DocumentListResponse.from(documents)
    }

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: String): DocumentDetailResponse {
        val principal = getPrincipal()
        val query = GetDocumentQuery(
            companyId = principal.companyId,
            documentId = DocumentId.from(id)
        )
        val document = getDocumentHandler.handle(principal, query)
        return DocumentDetailResponse.from(document)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteDocumentCommand(
            companyId = principal.companyId,
            documentId = DocumentId.from(id)
        )
        deleteDocumentHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }
}