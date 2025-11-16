// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/infrastructure/DocumentJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.document.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType

interface DocumentJpaRepository : JpaRepository<DocumentEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): DocumentEntity?

    @Query("""
        SELECT d FROM DocumentEntity d
        WHERE d.companyId = :companyId
        AND d.entityType = :entityType
        AND d.entityId = :entityId
        ORDER BY d.uploadedAt DESC
    """)
    fun findByEntity(
        @Param("companyId") companyId: String,
        @Param("entityType") entityType: EntityType,
        @Param("entityId") entityId: String
    ): List<DocumentEntity>

    @Query("""
        SELECT d FROM DocumentEntity d
        WHERE d.companyId = :companyId
        AND d.entityType = :entityType
        AND d.entityId = :entityId
        AND d.documentType = :documentType
        ORDER BY d.uploadedAt DESC
    """)
    fun findByEntityAndType(
        @Param("companyId") companyId: String,
        @Param("entityType") entityType: EntityType,
        @Param("entityId") entityId: String,
        @Param("documentType") documentType: DocumentType
    ): List<DocumentEntity>

    @Query("""
        SELECT COUNT(d) FROM DocumentEntity d
        WHERE d.companyId = :companyId
        AND d.entityType = :entityType
        AND d.entityId = :entityId
    """)
    fun countByEntity(
        @Param("companyId") companyId: String,
        @Param("entityType") entityType: EntityType,
        @Param("entityId") entityId: String
    ): Int

    fun findByS3Key(s3Key: String): DocumentEntity?
}