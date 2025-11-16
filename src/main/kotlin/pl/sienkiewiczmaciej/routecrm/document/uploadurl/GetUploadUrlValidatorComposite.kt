// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/uploadurl/GetUploadUrlValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.document.uploadurl

import org.springframework.stereotype.Component

@Component
class GetUploadUrlValidatorComposite(
    private val contextBuilder: GetUploadUrlContextBuilder,
    private val fileSizeValidator: UploadFileSizeValidator,
    private val fileNameValidator: UploadFileNameValidator,
    private val contentTypeValidator: UploadContentTypeValidator
) {
    suspend fun validate(command: GetUploadUrlCommand): GetUploadUrlValidationContext {
        val context = contextBuilder.build(command)

        fileSizeValidator.validate(command)
        fileNameValidator.validate(command)
        contentTypeValidator.validate(command)

        return context
    }
}

@Component
class UploadFileSizeValidator {
    fun validate(command: GetUploadUrlCommand) {
        require(command.fileSize > 0) {
            "File size must be positive"
        }

        require(command.fileSize <= 10 * 1024 * 1024) {
            "File size cannot exceed 10MB. Current size: ${command.fileSize / 1024 / 1024}MB"
        }
    }
}

@Component
class UploadFileNameValidator {
    fun validate(command: GetUploadUrlCommand) {
        require(command.fileName.isNotBlank()) {
            "File name is required"
        }

        require(command.fileName.length <= 255) {
            "File name too long. Maximum 255 characters"
        }

        val extension = command.fileName.substringAfterLast('.', "")
        require(extension.isNotBlank()) {
            "File must have an extension"
        }
    }
}

@Component
class UploadContentTypeValidator {
    fun validate(command: GetUploadUrlCommand) {
        val allowedTypes = setOf(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/heic",
            "image/heif"
        )

        require(command.contentType in allowedTypes) {
            "Content type ${command.contentType} is not allowed. Allowed: ${allowedTypes.joinToString()}"
        }
    }
}