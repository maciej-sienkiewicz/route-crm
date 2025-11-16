// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/getbyid/GetDocumentHandler.kt
package pl.sienkiewiczmaciej.routecrm.document.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentRepository
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class GetDocumentQuery(
    val companyId: CompanyId,
    val documentId: DocumentId
)

data class DocumentDetail(
    val id: DocumentId,
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
)

class DocumentNotFoundException(id: DocumentId) :
    NotFoundException("Document ${id.value} not found")

@Component
class GetDocumentHandler(
    private val documentRepository: DocumentRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDocumentQuery): DocumentDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val document = documentRepository.findById(query.companyId, query.documentId)
            ?: throw DocumentNotFoundException(query.documentId)

        // Role-specific authorization
        when (principal.role) {
            UserRole.DRIVER -> {
                require(principal.driverId != null) { "Driver must have driverId" }
                require(document.entityType == EntityType.DRIVER && document.entityId == principal.driverId) {
                    "Driver can only access their own documents"
                }
            }
            UserRole.GUARDIAN -> {
                require(principal.guardianId != null) { "Guardian must have guardianId" }
                // TODO: Verify guardian owns this entity
            }
            else -> { /* ADMIN/OPERATOR have full access */ }
        }

        return DocumentDetail(
            id = document.id,
            entityType = document.entityType,
            entityId = document.entityId,
            documentType = document.documentType,
            fileName = document.fileName,
            fileSize = document.fileSize,
            contentType = document.contentType,
            uploadedByName = document.uploadedByName,
            uploadedAt = document.uploadedAt,
            notes = document.notes,
            isPdf = document.isPdf(),
            isImage = document.isImage()
        )
    }
}