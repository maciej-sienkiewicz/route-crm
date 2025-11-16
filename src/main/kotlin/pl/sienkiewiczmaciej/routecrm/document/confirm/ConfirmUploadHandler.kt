// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/confirm/ConfirmUploadHandler.kt
package pl.sienkiewiczmaciej.routecrm.document.confirm

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class ConfirmUploadCommand(
    val companyId: CompanyId,
    val entityType: EntityType,
    val entityId: String,
    val documentType: DocumentType,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val s3Key: String,
    val notes: String?
)

data class ConfirmUploadResult(
    val id: DocumentId,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val s3Key: String
)

@Component
class ConfirmUploadHandler(
    private val validatorComposite: ConfirmUploadValidatorComposite,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: ConfirmUploadCommand): ConfirmUploadResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val context = validatorComposite.validate(command)

        val document = Document.create(
            companyId = command.companyId,
            entityType = command.entityType,
            entityId = command.entityId,
            documentType = command.documentType,
            fileName = command.fileName,
            fileSize = command.fileSize,
            contentType = command.contentType,
            s3Key = command.s3Key,
            uploadedBy = principal.userId,
            uploadedByName = "${principal.userId.value}",
            notes = command.notes
        )

        val saved = context.documentRepository.save(document)

        return ConfirmUploadResult(
            id = saved.id,
            fileName = saved.fileName,
            fileSize = saved.fileSize,
            contentType = saved.contentType,
            s3Key = saved.s3Key
        )
    }
}