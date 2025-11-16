// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/uploadurl/GetUploadUrlContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.document.uploadurl

import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.document.infrastructure.services.S3Service

data class GetUploadUrlValidationContext(
    val s3Service: S3Service
)

@Component
class GetUploadUrlContextBuilder(
    private val s3Service: S3Service
) {
    suspend fun build(command: GetUploadUrlCommand): GetUploadUrlValidationContext = coroutineScope {
        GetUploadUrlValidationContext(
            s3Service = s3Service
        )
    }
}