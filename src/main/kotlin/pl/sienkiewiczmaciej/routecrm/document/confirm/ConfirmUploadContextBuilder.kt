// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/confirm/ConfirmUploadContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.document.confirm

import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.document.domain.DocumentRepository
import pl.sienkiewiczmaciej.routecrm.document.infrastructure.services.S3Service

data class ConfirmUploadValidationContext(
    val documentRepository: DocumentRepository,
    val s3Service: S3Service
)

@Component
class ConfirmUploadContextBuilder(
    private val documentRepository: DocumentRepository,
    private val s3Service: S3Service
) {
    suspend fun build(command: ConfirmUploadCommand): ConfirmUploadValidationContext = coroutineScope {
        ConfirmUploadValidationContext(
            documentRepository = documentRepository,
            s3Service = s3Service
        )
    }
}