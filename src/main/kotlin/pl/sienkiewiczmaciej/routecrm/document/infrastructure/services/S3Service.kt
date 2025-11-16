// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/document/infrastructure/services/S3Service.kt
package pl.sienkiewiczmaciej.routecrm.document.infrastructure.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.document.domain.EntityType
import pl.sienkiewiczmaciej.routecrm.document.domain.PresignedUploadUrl
import pl.sienkiewiczmaciej.routecrm.document.domain.PresignedViewUrl
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.time.Instant

@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${aws.s3.bucket}") private val bucketName: String,
    @Value("\${aws.s3.region}") private val region: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun generateUploadUrl(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String,
        fileName: String,
        contentType: String
    ): PresignedUploadUrl {
        val key = buildS3Key(companyId, entityType, entityId, fileName)

        logger.info("Generating upload URL for key: $key")

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedRequest = s3Presigner.presignPutObject(presignRequest)

        return PresignedUploadUrl(
            url = presignedRequest.url().toString(),
            key = key,
            expiresIn = 900 // 15 minutes
        )
    }

    fun generateViewUrl(
        companyId: CompanyId,
        s3Key: String
    ): PresignedViewUrl {
        validateS3KeyBelongsToCompany(companyId, s3Key)

        logger.info("Generating view URL for key: $s3Key")

        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .getObjectRequest(getObjectRequest)
            .build()

        val presignedRequest = s3Presigner.presignGetObject(presignRequest)

        return PresignedViewUrl(
            url = presignedRequest.url().toString(),
            expiresIn = 900
        )
    }

    fun deleteObject(companyId: CompanyId, s3Key: String) {
        validateS3KeyBelongsToCompany(companyId, s3Key)

        logger.info("Deleting object from S3: $s3Key")

        val deleteRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()

        s3Client.deleteObject(deleteRequest)
    }

    fun objectExists(s3Key: String): Boolean {
        return try {
            val headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build()

            s3Client.headObject(headRequest)
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }

    private fun buildS3Key(
        companyId: CompanyId,
        entityType: EntityType,
        entityId: String,
        fileName: String
    ): String {
        val sanitizedFileName = sanitizeFileName(fileName)
        val timestamp = Instant.now().epochSecond
        val randomSuffix = (1000..9999).random()

        return when (entityType) {
            EntityType.DRIVER -> "drivers/${companyId.value}/$entityId/$timestamp-$randomSuffix-$sanitizedFileName"
            EntityType.VEHICLE -> "vehicles/${companyId.value}/$entityId/$timestamp-$randomSuffix-$sanitizedFileName"
            EntityType.CHILD -> "children/${companyId.value}/$entityId/$timestamp-$randomSuffix-$sanitizedFileName"
            EntityType.GUARDIAN -> "guardians/${companyId.value}/$entityId/$timestamp-$randomSuffix-$sanitizedFileName"
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100)
    }

    private fun validateS3KeyBelongsToCompany(companyId: CompanyId, s3Key: String) {
        val validPrefixes = listOf(
            "drivers/${companyId.value}/",
            "vehicles/${companyId.value}/",
            "children/${companyId.value}/",
            "guardians/${companyId.value}/"
        )

        require(validPrefixes.any { s3Key.startsWith(it) }) {
            "S3 key does not belong to company ${companyId.value}"
        }
    }
}