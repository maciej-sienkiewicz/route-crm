// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/list/ListDocumentsHandler.kt
package pl.sienkiewiczmaciej.routecrm.document.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentRepository
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class ListDocumentsQuery(
    val companyId: CompanyId,
    val entityType: EntityType,
    val entityId: String,
    val documentType: DocumentType?
)

data class DocumentListItem(
    val id: DocumentId,
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

@Component
class ListDocumentsHandler(
    private val documentRepository: DocumentRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListDocumentsQuery): List<DocumentListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        // Role-specific authorization
        when (principal.role) {
            UserRole.DRIVER -> {
                require(principal.driverId != null) { "Driver must have driverId" }
                require(query.entityType == EntityType.DRIVER && query.entityId == principal.driverId) {
                    "Driver can only list their own documents"
                }
            }
            UserRole.GUARDIAN -> {
                require(principal.guardianId != null) { "Guardian must have guardianId" }
                // TODO: Verify guardian can access this entity
            }
            else -> { /* ADMIN/OPERATOR have full access */ }
        }

        val documents = if (query.documentType != null) {
            documentRepository.findByEntityAndType(
                companyId = query.companyId,
                entityType = query.entityType,
                entityId = query.entityId,
                documentType = query.documentType
            )
        } else {
            documentRepository.findByEntity(
                companyId = query.companyId,
                entityType = query.entityType,
                entityId = query.entityId
            )
        }

        return documents.map { document ->
            DocumentListItem(
                id = document.id,
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
}