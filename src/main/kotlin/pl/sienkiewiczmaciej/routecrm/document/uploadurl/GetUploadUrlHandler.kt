// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/uploadurl/GetUploadUrlHandler.kt
package pl.sienkiewiczmaciej.routecrm.document.uploadurl

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class GetUploadUrlCommand(
    val companyId: CompanyId,
    val entityType: EntityType,
    val entityId: String,
    val documentType: DocumentType,
    val fileName: String,
    val contentType: String,
    val fileSize: Long
)

data class GetUploadUrlResult(
    val uploadUrl: String,
    val s3Key: String,
    val expiresIn: Int
)

@Component
class GetUploadUrlHandler(
    private val validatorComposite: GetUploadUrlValidatorComposite,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, command: GetUploadUrlCommand): GetUploadUrlResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val context = validatorComposite.validate(command)

        val presignedUrl = context.s3Service.generateUploadUrl(
            companyId = command.companyId,
            entityType = command.entityType,
            entityId = command.entityId,
            fileName = command.fileName,
            contentType = command.contentType
        )

        return GetUploadUrlResult(
            uploadUrl = presignedUrl.url,
            s3Key = presignedUrl.key,
            expiresIn = presignedUrl.expiresIn
        )
    }
}

