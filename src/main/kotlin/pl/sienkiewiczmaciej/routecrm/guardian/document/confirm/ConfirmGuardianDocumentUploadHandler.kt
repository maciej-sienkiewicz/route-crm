package pl.sienkiewiczmaciej.routecrm.guardian.document.confirm

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.document.domain.*
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class ConfirmGuardianDocumentUploadCommand(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val s3Key: String,
    val documentType: DocumentType,
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
class ConfirmGuardianDocumentUploadHandler(
    private val guardianRepository: GuardianRepository,
    private val documentRepository: DocumentRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: ConfirmGuardianDocumentUploadCommand): ConfirmUploadResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        guardianRepository.findById(command.companyId, command.guardianId)
            ?: throw GuardianNotFoundException(command.guardianId)

        val document = Document.create(
            companyId = command.companyId,
            entityType = EntityType.GUARDIAN,
            entityId = command.guardianId.value,
            documentType = command.documentType,
            fileName = command.fileName,
            fileSize = command.fileSize,
            contentType = command.contentType,
            s3Key = command.s3Key,
            uploadedBy = principal.userId,
            uploadedByName = "${principal.firstName} ${principal.lastName}",
            notes = command.notes
        )

        val saved = documentRepository.save(document)

        return ConfirmUploadResult(
            id = saved.id,
            fileName = saved.fileName,
            fileSize = saved.fileSize,
            contentType = saved.contentType,
            s3Key = saved.s3Key
        )
    }
}