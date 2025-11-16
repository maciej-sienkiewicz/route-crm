// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/delete/DeleteDocumentHandler.kt
package pl.sienkiewiczmaciej.routecrm.document.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentRepository
import pl.sienkiewiczmaciej.routecrm.document.getbyid.DocumentNotFoundException
import pl.sienkiewiczmaciej.routecrm.document.infrastructure.services.S3Service
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteDocumentCommand(
    val companyId: CompanyId,
    val documentId: DocumentId
)

@Component
class DeleteDocumentHandler(
    private val documentRepository: DocumentRepository,
    private val s3Service: S3Service,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteDocumentCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val document = documentRepository.findById(command.companyId, command.documentId)
            ?: throw DocumentNotFoundException(command.documentId)

        // Delete from S3 first
        try {
            s3Service.deleteObject(command.companyId, document.s3Key)
        } catch (e: Exception) {
            // Log error but continue with database deletion
            // This prevents orphaned database records if S3 deletion fails
        }

        // Delete from database
        documentRepository.delete(command.companyId, command.documentId)
    }
}