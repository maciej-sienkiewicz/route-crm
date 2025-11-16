// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/infrastructure/DocumentEntity.kt
package pl.sienkiewiczmaciej.routecrm.document.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.document.domain.Document
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant

@Entity
@Table(
    name = "documents",
    indexes = [
        Index(name = "idx_documents_company", columnList = "company_id"),
        Index(name = "idx_documents_entity", columnList = "company_id, entity_type, entity_id"),
        Index(name = "idx_documents_type", columnList = "company_id, document_type"),
        Index(name = "idx_documents_s3key", columnList = "s3_key"),
        Index(name = "idx_documents_uploaded", columnList = "company_id, uploaded_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_documents_s3key", columnNames = ["s3_key"])
    ]
)
class DocumentEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    val entityType: EntityType,

    @Column(name = "entity_id", nullable = false, length = 50)
    val entityId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    val documentType: DocumentType,

    @Column(name = "file_name", nullable = false, length = 255)
    val fileName: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "content_type", nullable = false, length = 100)
    val contentType: String,

    @Column(name = "s3_key", nullable = false, length = 500)
    val s3Key: String,

    @Column(name = "uploaded_by", nullable = false, length = 50)
    val uploadedBy: String,

    @Column(name = "uploaded_by_name", nullable = false, length = 255)
    val uploadedByName: String,

    @Column(name = "uploaded_at", nullable = false)
    val uploadedAt: Instant = Instant.now(),

    @Column(columnDefinition = "text")
    val notes: String?
) {
    fun toDomain() = Document(
        id = DocumentId(id),
        companyId = CompanyId(companyId),
        entityType = entityType,
        entityId = entityId,
        documentType = documentType,
        fileName = fileName,
        fileSize = fileSize,
        contentType = contentType,
        s3Key = s3Key,
        uploadedBy = UserId(uploadedBy),
        uploadedByName = uploadedByName,
        uploadedAt = uploadedAt,
        notes = notes
    )

    companion object {
        fun fromDomain(document: Document) = DocumentEntity(
            id = document.id.value,
            companyId = document.companyId.value,
            entityType = document.entityType,
            entityId = document.entityId,
            documentType = document.documentType,
            fileName = document.fileName,
            fileSize = document.fileSize,
            contentType = document.contentType,
            s3Key = document.s3Key,
            uploadedBy = document.uploadedBy.value,
            uploadedByName = document.uploadedByName,
            uploadedAt = document.uploadedAt,
            notes = document.notes
        )
    }
}