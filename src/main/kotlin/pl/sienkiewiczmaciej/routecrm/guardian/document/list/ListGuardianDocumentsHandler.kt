package pl.sienkiewiczmaciej.routecrm.guardian.document.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentRepository
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class ListGuardianDocumentsQuery(
    val companyId: CompanyId,
    val guardianId: GuardianId
)

data class GuardianDocumentListItem(
    val id: DocumentId,
    val documentType: DocumentType,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val uploadedByName: String,
    val uploadedAt: Instant,
    val notes: String?
)

@Component
class ListGuardianDocumentsHandler(
    private val documentRepository: DocumentRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListGuardianDocumentsQuery): List<GuardianDocumentListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val documents = documentRepository.findByEntity(
            companyId = query.companyId,
            entityType = EntityType.GUARDIAN,
            entityId = query.guardianId.value
        )

        return documents.map { document ->
            GuardianDocumentListItem(
                id = document.id,
                documentType = document.documentType,
                fileName = document.fileName,
                fileSize = document.fileSize,
                contentType = document.contentType,
                uploadedByName = document.uploadedByName,
                uploadedAt = document.uploadedAt,
                notes = document.notes
            )
        }
    }
}