package pl.sienkiewiczmaciej.routecrm.guardian.document.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentRepository
import pl.sienkiewiczmaciej.routecrm.document.infrastructure.services.S3Service
import pl.sienkiewiczmaciej.routecrm.guardian.document.viewurl.DocumentNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteGuardianDocumentCommand(
    val companyId: CompanyId,
    val documentId: DocumentId
)

@Component
class DeleteGuardianDocumentHandler(
    private val documentRepository: DocumentRepository,
    private val s3Service: S3Service,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteGuardianDocumentCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val document = documentRepository.findById(command.companyId, command.documentId)
            ?: throw DocumentNotFoundException(command.documentId)

        s3Service.deleteObject(command.companyId, document.s3Key)
        documentRepository.delete(command.companyId, command.documentId)
    }
}