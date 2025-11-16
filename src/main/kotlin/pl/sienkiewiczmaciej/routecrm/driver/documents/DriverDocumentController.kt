// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/documents/DriverDocumentController.kt
package pl.sienkiewiczmaciej.routecrm.driver.documents

import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.document.*
import pl.sienkiewiczmaciej.routecrm.document.confirm.ConfirmUploadHandler
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.document.list.ListDocumentsHandler
import pl.sienkiewiczmaciej.routecrm.document.list.ListDocumentsQuery
import pl.sienkiewiczmaciej.routecrm.document.uploadurl.GetUploadUrlHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/drivers")
class DriverDocumentController(
    private val getUploadUrlHandler: GetUploadUrlHandler,
    private val confirmUploadHandler: ConfirmUploadHandler,
    private val listDocumentsHandler: ListDocumentsHandler
) : BaseController() {

    @PostMapping("/{driverId}/documents/upload-url")
    suspend fun getUploadUrl(
        @PathVariable driverId: String,
        @Valid @RequestBody request: GetUploadUrlRequest
    ): ResponseEntity<UploadUrlResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId).copy(
            entityType = EntityType.DRIVER,
            entityId = driverId
        )
        val result = getUploadUrlHandler.handle(principal, command)
        return ResponseEntity.ok(UploadUrlResponse.from(result))
    }

    @PostMapping("/{driverId}/documents")
    suspend fun confirmUpload(
        @PathVariable driverId: String,
        @Valid @RequestBody request: ConfirmUploadRequest
    ): ResponseEntity<DocumentResponse> {
        val principal = getPrincipal()

        // ✅ Przekazujemy entityType i entityId bezpośrednio do toCommand
        val command = request.toCommand(
            companyId = principal.companyId,
            entityType = EntityType.DRIVER,
            entityId = driverId
        )

        val result = confirmUploadHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(DocumentResponse.from(result))
    }

    @GetMapping("/{driverId}/documents")
    suspend fun list(@PathVariable driverId: String): DocumentListResponse {
        val principal = getPrincipal()
        val query = ListDocumentsQuery(
            companyId = principal.companyId,
            entityType = EntityType.DRIVER,
            entityId = driverId,
            documentType = null
        )
        val documents = listDocumentsHandler.handle(principal, query)
        return DocumentListResponse.from(documents)
    }
}