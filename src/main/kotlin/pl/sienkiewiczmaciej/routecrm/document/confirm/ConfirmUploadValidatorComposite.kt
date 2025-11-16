// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/confirm/ConfirmUploadValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.document.confirm

import org.springframework.stereotype.Component

@Component
class ConfirmUploadValidatorComposite(
    private val contextBuilder: ConfirmUploadContextBuilder,
    private val s3ObjectExistsValidator: S3ObjectExistsValidator
) {
    suspend fun validate(command: ConfirmUploadCommand): ConfirmUploadValidationContext {
        val context = contextBuilder.build(command)

        s3ObjectExistsValidator.validate(command, context)

        return context
    }
}

@Component
class S3ObjectExistsValidator {
    fun validate(command: ConfirmUploadCommand, context: ConfirmUploadValidationContext) {
        val exists = context.s3Service.objectExists(command.s3Key)

        require(exists) {
            "File was not uploaded to S3. Key: ${command.s3Key}"
        }
    }
}