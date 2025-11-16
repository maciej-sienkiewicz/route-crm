// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/domain/DocumentDomainModels.kt
package pl.sienkiewiczmaciej.routecrm.document.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant
import java.util.*

@JvmInline
value class DocumentId(val value: String) {
    companion object {
        fun generate() = DocumentId("DOC-${UUID.randomUUID()}")
        fun from(value: String) = DocumentId(value)
    }
}

enum class EntityType {
    DRIVER,
    VEHICLE,
    CHILD,
    GUARDIAN
}

enum class DocumentType {
    // Driver documents
    DRIVER_CONTRACT,
    DRIVER_CONTRACT_AMENDMENT,
    DRIVER_LICENSE_SCAN,
    DRIVER_ID_SCAN,
    DRIVER_MEDICAL_CERTIFICATE,
    DRIVER_LEAVE_REQUEST,
    DRIVER_OTHER,

    // Vehicle documents
    VEHICLE_PHOTO,
    VEHICLE_INSURANCE,
    VEHICLE_REGISTRATION,
    VEHICLE_INSPECTION,
    VEHICLE_OTHER,

    // Child documents
    CHILD_CONSENT,
    CHILD_MEDICAL_INFO,
    CHILD_PHOTO,
    CHILD_OTHER,

    // Guardian documents
    GUARDIAN_ID_SCAN,
    GUARDIAN_CONSENT,
    GUARDIAN_OTHER,

    // General
    OTHER
}

data class Document(
    val id: DocumentId,
    val companyId: CompanyId,
    val entityType: EntityType,
    val entityId: String,
    val documentType: DocumentType,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val s3Key: String,
    val uploadedBy: UserId,
    val uploadedByName: String,
    val uploadedAt: Instant,
    val notes: String?
) {
    companion object {
        fun create(
            companyId: CompanyId,
            entityType: EntityType,
            entityId: String,
            documentType: DocumentType,
            fileName: String,
            fileSize: Long,
            contentType: String,
            s3Key: String,
            uploadedBy: UserId,
            uploadedByName: String,
            notes: String?
        ): Document {
            require(fileName.isNotBlank()) { "File name is required" }
            require(fileSize > 0) { "File size must be positive" }
            require(fileSize <= 10 * 1024 * 1024) { "File size cannot exceed 10MB" }
            require(contentType.isNotBlank()) { "Content type is required" }
            require(s3Key.isNotBlank()) { "S3 key is required" }

            validateContentType(contentType)

            return Document(
                id = DocumentId.generate(),
                companyId = companyId,
                entityType = entityType,
                entityId = entityId,
                documentType = documentType,
                fileName = fileName.trim(),
                fileSize = fileSize,
                contentType = contentType,
                s3Key = s3Key,
                uploadedBy = uploadedBy,
                uploadedByName = uploadedByName,
                uploadedAt = Instant.now(),
                notes = notes?.trim()
            )
        }

        private fun validateContentType(contentType: String) {
            val allowedTypes = setOf(
                "application/pdf",
                "image/jpeg",
                "image/jpg",
                "image/png",
                "image/heic",
                "image/heif"
            )

            require(contentType in allowedTypes) {
                "Content type $contentType is not allowed. Allowed types: $allowedTypes"
            }
        }
    }

    fun isPdf(): Boolean = contentType == "application/pdf"

    fun isImage(): Boolean = contentType.startsWith("image/")

    fun belongsToCompany(companyId: CompanyId): Boolean = this.companyId == companyId
}

data class PresignedUploadUrl(
    val url: String,
    val key: String,
    val expiresIn: Int
)

data class PresignedViewUrl(
    val url: String,
    val expiresIn: Int
)