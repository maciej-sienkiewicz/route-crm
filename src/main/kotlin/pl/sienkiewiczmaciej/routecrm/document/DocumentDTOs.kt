// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/DocumentDTOs.kt
package pl.sienkiewiczmaciej.routecrm.document

import jakarta.validation.constraints.*
import pl.sienkiewiczmaciej.routecrm.document.confirm.ConfirmUploadCommand
import pl.sienkiewiczmaciej.routecrm.document.confirm.ConfirmUploadResult
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.document.getbyid.DocumentDetail
import pl.sienkiewiczmaciej.routecrm.document.list.DocumentListItem
import pl.sienkiewiczmaciej.routecrm.document.uploadurl.GetUploadUrlCommand
import pl.sienkiewiczmaciej.routecrm.document.uploadurl.GetUploadUrlResult
import pl.sienkiewiczmaciej.routecrm.document.viewurl.GetViewUrlResult
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

data class GetUploadUrlRequest(
    @field:NotNull(message = "Entity type is required")
    val entityType: EntityType,

    @field:NotBlank(message = "Entity ID is required")
    @field:Size(max = 50)
    val entityId: String,

    @field:NotNull(message = "Document type is required")
    val documentType: DocumentType,

    @field:NotBlank(message = "File name is required")
    @field:Size(max = 255)
    val fileName: String,

    @field:NotBlank(message = "Content type is required")
    @field:Size(max = 100)
    val contentType: String,

    @field:NotNull(message = "File size is required")
    @field:Positive(message = "File size must be positive")
    @field:Max(value = 10485760, message = "File size cannot exceed 10MB")
    val fileSize: Long
) {
    fun toCommand(companyId: CompanyId) = GetUploadUrlCommand(
        companyId = companyId,
        entityType = entityType,
        entityId = entityId,
        documentType = documentType,
        fileName = fileName,
        contentType = contentType,
        fileSize = fileSize
    )
}

data class UploadUrlResponse(
    val uploadUrl: String,
    val s3Key: String,
    val expiresIn: Int
) {
    companion object {
        fun from(result: GetUploadUrlResult) = UploadUrlResponse(
            uploadUrl = result.uploadUrl,
            s3Key = result.s3Key,
            expiresIn = result.expiresIn
        )
    }
}

// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/DocumentDTOs.kt

data class ConfirmUploadRequest(
    // ❌ USUŃ entityType - nie jest potrzebne
    // ❌ USUŃ entityId - nie jest potrzebne

    @field:NotNull(message = "Document type is required")
    val documentType: DocumentType,

    @field:NotBlank(message = "File name is required")
    @field:Size(max = 255)
    val fileName: String,

    @field:NotNull(message = "File size is required")
    @field:Positive
    val fileSize: Long,

    @field:NotBlank(message = "Content type is required")
    val contentType: String,

    @field:NotBlank(message = "S3 key is required")
    @field:Size(max = 500)
    val s3Key: String,

    @field:Size(max = 2000)
    val notes: String?
) {
    fun toCommand(companyId: CompanyId, entityType: EntityType, entityId: String) = ConfirmUploadCommand(
        companyId = companyId,
        entityType = entityType,
        entityId = entityId,
        documentType = documentType,
        fileName = fileName,
        fileSize = fileSize,
        contentType = contentType,
        s3Key = s3Key,
        notes = notes
    )
}

data class DocumentResponse(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val s3Key: String
) {
    companion object {
        fun from(result: ConfirmUploadResult) = DocumentResponse(
            id = result.id.value,
            fileName = result.fileName,
            fileSize = result.fileSize,
            contentType = result.contentType,
            s3Key = result.s3Key
        )
    }
}

data class ViewUrlResponse(
    val viewUrl: String,
    val expiresIn: Int
) {
    companion object {
        fun from(result: GetViewUrlResult) = ViewUrlResponse(
            viewUrl = result.viewUrl,
            expiresIn = result.expiresIn
        )
    }
}

data class DocumentListItemResponse(
    val id: String,
    val documentType: DocumentType,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val uploadedByName: String,
    val uploadedAt: Instant,
    val notes: String?,
    val isPdf: Boolean,
    val isImage: Boolean
) {
    companion object {
        fun from(item: DocumentListItem) = DocumentListItemResponse(
            id = item.id.value,
            documentType = item.documentType,
            fileName = item.fileName,
            fileSize = item.fileSize,
            contentType = item.contentType,
            uploadedByName = item.uploadedByName,
            uploadedAt = item.uploadedAt,
            notes = item.notes,
            isPdf = item.isPdf,
            isImage = item.isImage
        )
    }
}

data class DocumentListResponse(
    val documents: List<DocumentListItemResponse>,
    val totalCount: Int
) {
    companion object {
        fun from(documents: List<DocumentListItem>) = DocumentListResponse(
            documents = documents.map { DocumentListItemResponse.from(it) },
            totalCount = documents.size
        )
    }
}

data class DocumentDetailResponse(
    val id: String,
    val entityType: EntityType,
    val entityId: String,
    val documentType: DocumentType,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val uploadedByName: String,
    val uploadedAt: Instant,
    val notes: String?,
    val isPdf: Boolean,
    val isImage: Boolean
) {
    companion object {
        fun from(detail: DocumentDetail) = DocumentDetailResponse(
            id = detail.id.value,
            entityType = detail.entityType,
            entityId = detail.entityId,
            documentType = detail.documentType,
            fileName = detail.fileName,
            fileSize = detail.fileSize,
            contentType = detail.contentType,
            uploadedByName = detail.uploadedByName,
            uploadedAt = detail.uploadedAt,
            notes = detail.notes,
            isPdf = detail.isPdf,
            isImage = detail.isImage
        )
    }
}