// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/infrastructure/DocumentRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.document.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.document.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class DocumentRepositoryImpl(
    private val jpaRepository: DocumentJpaRepository
) : DocumentRepository {

    override suspend fun save(document: Document): Document = withContext(Dispatchers.IO) {
        val entity = DocumentEntity.fromDomain(document)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: DocumentId): Document? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByEntity(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String
    ): List<Document> = withContext(Dispatchers.IO) {
        jpaRepository.findByEntity(companyId.value, entityType, entityId)
            .map { it.toDomain() }
    }

    override suspend fun findByEntityAndType(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String,
        documentType: DocumentType
    ): List<Document> = withContext(Dispatchers.IO) {
        jpaRepository.findByEntityAndType(companyId.value, entityType, entityId, documentType)
            .map { it.toDomain() }
    }

    override suspend fun delete(companyId: CompanyId, id: DocumentId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }

    override suspend fun countByEntity(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String
    ): Int = withContext(Dispatchers.IO) {
        jpaRepository.countByEntity(companyId.value, entityType, entityId)
    }
}