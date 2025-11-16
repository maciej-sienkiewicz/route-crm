// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/viewurl/GetViewUrlHandler.kt
package pl.sienkiewiczmaciej.routecrm.document.viewurl

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

data class GetViewUrlCommand(
    val companyId: CompanyId,
    val documentId: DocumentId
)

data class GetViewUrlResult(
    val viewUrl: String,
    val expiresIn: Int
)

@Component
class GetViewUrlHandler(
    private val documentRepository: DocumentRepository,
    private val s3Service: S3Service,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, command: GetViewUrlCommand): GetViewUrlResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val document = documentRepository.findById(command.companyId, command.documentId)
            ?: throw DocumentNotFoundException(command.documentId)

        // Role-specific authorization
        when (principal.role) {
            UserRole.DRIVER -> {
                require(principal.driverId != null) { "Driver must have driverId" }
                require(document.entityType == pl.sienkiewiczmaciej.routecrm.document.domain.EntityType.DRIVER) {
                    "Driver can only access driver documents"
                }
                require(document.entityId == principal.driverId) {
                    "Driver can only access their own documents"
                }
            }
            UserRole.GUARDIAN -> {
                require(principal.guardianId != null) { "Guardian must have guardianId" }
                require(
                    document.entityType == pl.sienkiewiczmaciej.routecrm.document.domain.EntityType.GUARDIAN ||
                            document.entityType == pl.sienkiewiczmaciej.routecrm.document.domain.EntityType.CHILD
                ) {
                    "Guardian can only access guardian or child documents"
                }
                // TODO: Verify guardian owns the child
            }
            else -> { /* ADMIN/OPERATOR have full access */ }
        }

        val presignedUrl = s3Service.generateViewUrl(command.companyId, document.s3Key)

        return GetViewUrlResult(
            viewUrl = presignedUrl.url,
            expiresIn = presignedUrl.expiresIn
        )
    }
}