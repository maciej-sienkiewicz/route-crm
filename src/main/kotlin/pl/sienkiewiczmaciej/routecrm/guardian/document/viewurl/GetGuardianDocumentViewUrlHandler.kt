package pl.sienkiewiczmaciej.routecrm.guardian.document.viewurl

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentId
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentRepository
import pl.sienkiewiczmaciej.routecrm.document.infrastructure.services.S3Service
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class GetGuardianDocumentViewUrlCommand(
    val companyId: CompanyId,
    val documentId: DocumentId
)

data class GetViewUrlResult(
    val viewUrl: String,
    val expiresIn: Int
)

class DocumentNotFoundException(id: DocumentId) : NotFoundException("Document ${id.value} not found")

@Component
class GetGuardianDocumentViewUrlHandler(
    private val documentRepository: DocumentRepository,
    private val s3Service: S3Service,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, command: GetGuardianDocumentViewUrlCommand): GetViewUrlResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val document = documentRepository.findById(command.companyId, command.documentId)
            ?: throw DocumentNotFoundException(command.documentId)

        val presignedUrl = s3Service.generateViewUrl(
            companyId = command.companyId,
            s3Key = document.s3Key
        )

        return GetViewUrlResult(
            viewUrl = presignedUrl.url,
            expiresIn = presignedUrl.expiresIn
        )
    }
}