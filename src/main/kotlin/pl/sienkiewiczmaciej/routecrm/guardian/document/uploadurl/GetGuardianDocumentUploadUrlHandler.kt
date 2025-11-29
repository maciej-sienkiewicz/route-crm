package pl.sienkiewiczmaciej.routecrm.guardian.document.uploadurl

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentType
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.document.infrastructure.services.S3Service
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class GetGuardianDocumentUploadUrlCommand(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val documentType: DocumentType,
    val notes: String?
)

data class GetUploadUrlResult(
    val uploadUrl: String,
    val s3Key: String,
    val expiresIn: Int
)

@Component
class GetGuardianDocumentUploadUrlHandler(
    private val guardianRepository: GuardianRepository,
    private val s3Service: S3Service,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, command: GetGuardianDocumentUploadUrlCommand): GetUploadUrlResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        guardianRepository.findById(command.companyId, command.guardianId)
            ?: throw GuardianNotFoundException(command.guardianId)

        require(command.fileSize <= 10 * 1024 * 1024) {
            "File size cannot exceed 10MB"
        }

        val presignedUrl = s3Service.generateUploadUrl(
            companyId = command.companyId,
            entityType = EntityType.GUARDIAN,
            entityId = command.guardianId.value,
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