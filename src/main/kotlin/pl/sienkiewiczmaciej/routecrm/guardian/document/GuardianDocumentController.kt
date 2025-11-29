// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/guardian/document/GuardianDocumentController.kt
package pl.sienkiewiczmaciej.routecrm.guardian.document

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.guardian.document.confirm.ConfirmGuardianDocumentUploadCommand
import pl.sienkiewiczmaciej.routecrm.guardian.document.confirm.ConfirmGuardianDocumentUploadHandler
import pl.sienkiewiczmaciej.routecrm.guardian.document.delete.DeleteGuardianDocumentCommand
import pl.sienkiewiczmaciej.routecrm.guardian.document.delete.DeleteGuardianDocumentHandler
import pl.sienkiewiczmaciej.routecrm.guardian.document.list.ListGuardianDocumentsHandler
import pl.sienkiewiczmaciej.routecrm.guardian.document.list.ListGuardianDocumentsQuery
import pl.sienkiewiczmaciej.routecrm.guardian.document.uploadurl.GetGuardianDocumentUploadUrlCommand
import pl.sienkiewiczmaciej.routecrm.guardian.document.uploadurl.GetGuardianDocumentUploadUrlHandler
import pl.sienkiewiczmaciej.routecrm.guardian.document.viewurl.GetGuardianDocumentViewUrlCommand
import pl.sienkiewiczmaciej.routecrm.guardian.document.viewurl.GetGuardianDocumentViewUrlHandler
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

data class GetDocumentUploadUrlRequest(
    @field:NotBlank(message = "File name is required")
    @field:Size(max = 255)
    val fileName: String,

    @field:Positive(message = "File size must be positive")
    val fileSize: Long,

    @field:NotBlank(message = "Content type is required")
    val contentType: String,

    @field:NotNull(message = "Document type is required")
    val documentType: DocumentType,

    @field:Size(max = 1000)
    val notes: String?
) {
    fun toCommand(companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId, guardianId: GuardianId) =
        GetGuardianDocumentUploadUrlCommand(
            companyId = companyId,
            guardianId = guardianId,
            fileName = fileName,
            fileSize = fileSize,
            contentType = contentType,
            documentType = documentType,
            notes = notes
        )
}

data class UploadUrlResponse(
    val uploadUrl: String,
    val s3Key: String,
    val expiresIn: Int
) {
    companion object {
        fun from(result: pl.sienkiewiczmaciej.routecrm.guardian.document.uploadurl.GetUploadUrlResult) =
            UploadUrlResponse(
                uploadUrl = result.uploadUrl,
                s3Key = result.s3Key,
                expiresIn = result.expiresIn
            )
    }
}

data class ConfirmDocumentUploadRequest(
    @field:NotBlank(message = "File name is required")
    val fileName: String,

    @field:Positive(message = "File size must be positive")
    val fileSize: Long,

    @field:NotBlank(message = "Content type is required")
    val contentType: String,

    @field:NotBlank(message = "S3 key is required")
    val s3Key: String,

    @field:NotNull(message = "Document type is required")
    val documentType: DocumentType,

    val notes: String?
) {
    fun toCommand(companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId, guardianId: GuardianId) =
        ConfirmGuardianDocumentUploadCommand(
            companyId = companyId,
            guardianId = guardianId,
            fileName = fileName,
            fileSize = fileSize,
            contentType = contentType,
            s3Key = s3Key,
            documentType = documentType,
            notes = notes
        )
}

data class DocumentUploadConfirmationResponse(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val s3Key: String
) {
    companion object {
        fun from(result: pl.sienkiewiczmaciej.routecrm.guardian.document.confirm.ConfirmUploadResult) =
            DocumentUploadConfirmationResponse(
                id = result.id.value,
                fileName = result.fileName,
                fileSize = result.fileSize,
                contentType = result.contentType,
                s3Key = result.s3Key
            )
    }
}

data class GuardianDocumentResponse(
    val id: String,
    val documentType: DocumentType,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val uploadedByName: String,
    val uploadedAt: String,
    val notes: String?,
    val isPdf: Boolean,
    val isImage: Boolean
) {
    companion object {
        fun from(item: pl.sienkiewiczmaciej.routecrm.guardian.document.list.GuardianDocumentListItem) =
            GuardianDocumentResponse(
                id = item.id.value,
                documentType = item.documentType,
                fileName = item.fileName,
                fileSize = item.fileSize,
                contentType = item.contentType,
                uploadedByName = item.uploadedByName,
                uploadedAt = item.uploadedAt.toString(),
                notes = item.notes,
                isPdf = item.contentType == "application/pdf",
                isImage = item.contentType.startsWith("image/")
            )
    }
}

data class DocumentViewUrlResponse(
    val viewUrl: String,
    val expiresIn: Int
) {
    companion object {
        fun from(result: pl.sienkiewiczmaciej.routecrm.guardian.document.viewurl.GetViewUrlResult) =
            DocumentViewUrlResponse(
                viewUrl = result.viewUrl,
                expiresIn = result.expiresIn
            )
    }
}

@RestController
@RequestMapping("/api/guardians")
class GuardianDocumentController(
    private val getUploadUrlHandler: GetGuardianDocumentUploadUrlHandler,
    private val confirmUploadHandler: ConfirmGuardianDocumentUploadHandler,
    private val listDocumentsHandler: ListGuardianDocumentsHandler,
    private val getViewUrlHandler: GetGuardianDocumentViewUrlHandler,
    private val deleteDocumentHandler: DeleteGuardianDocumentHandler
) : BaseController() {

    @PostMapping("/{guardianId}/documents/upload-url")
    suspend fun getUploadUrl(
        @PathVariable guardianId: String,
        @Valid @RequestBody request: GetDocumentUploadUrlRequest
    ): UploadUrlResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, GuardianId.from(guardianId))
        val result = getUploadUrlHandler.handle(principal, command)
        return UploadUrlResponse.from(result)
    }

    @PostMapping("/{guardianId}/documents")
    suspend fun confirmUpload(
        @PathVariable guardianId: String,
        @Valid @RequestBody request: ConfirmDocumentUploadRequest
    ): ResponseEntity<DocumentUploadConfirmationResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, GuardianId.from(guardianId))
        val result = confirmUploadHandler.handle(principal, command)
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentUploadConfirmationResponse.from(result))
    }

    @GetMapping("/{guardianId}/documents")
    suspend fun listDocuments(@PathVariable guardianId: String): List<GuardianDocumentResponse> {
        val principal = getPrincipal()
        val query = ListGuardianDocumentsQuery(
            companyId = principal.companyId,
            guardianId = GuardianId.from(guardianId)
        )
        return listDocumentsHandler.handle(principal, query).map { GuardianDocumentResponse.from(it) }
    }

    @GetMapping("/documents/{documentId}/view-url")
    suspend fun getViewUrl(@PathVariable documentId: String): DocumentViewUrlResponse {
        val principal = getPrincipal()
        val command = GetGuardianDocumentViewUrlCommand(
            companyId = principal.companyId,
            documentId = DocumentId.from(documentId)
        )
        val result = getViewUrlHandler.handle(principal, command)
        return DocumentViewUrlResponse.from(result)
    }

    @DeleteMapping("/documents/{documentId}")
    suspend fun deleteDocument(@PathVariable documentId: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteGuardianDocumentCommand(
            companyId = principal.companyId,
            documentId = DocumentId.from(documentId)
        )
        deleteDocumentHandler.handle(principal, command)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}