// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/domain/DocumentRepository.kt
package pl.sienkiewiczmaciej.routecrm.document.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface DocumentRepository {
    suspend fun save(document: Document): Document

    suspend fun findById(companyId: CompanyId, id: DocumentId): Document?

    suspend fun findByEntity(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String
    ): List<Document>

    suspend fun findByEntityAndType(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String,
        documentType: DocumentType
    ): List<Document>

    suspend fun delete(companyId: CompanyId, id: DocumentId)

    suspend fun countByEntity(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String
    ): Int
}