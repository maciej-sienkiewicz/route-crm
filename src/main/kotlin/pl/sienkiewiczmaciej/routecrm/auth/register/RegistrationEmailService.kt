package pl.sienkiewiczmaciej.routecrm.auth.register

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.auth.global.VerificationToken
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface RegistrationEmailService {
    suspend fun sendVerificationEmail(
        email: String,
        firstName: String,
        token: VerificationToken,
        companyId: CompanyId
    )

    suspend fun sendCompanyLinkingEmail(
        email: String,
        token: VerificationToken,
        companyId: CompanyId,
        pendingData: PendingGuardianData
    )
}

@Service
class RegistrationEmailServiceImpl : RegistrationEmailService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun sendVerificationEmail(
        email: String,
        firstName: String,
        token: VerificationToken,
        companyId: CompanyId
    ) {
        logger.info("Sending verification email to $email for company ${companyId.value}")
        logger.info("Verification link: /api/auth/verify-email?token=${token.value}")
    }

    override suspend fun sendCompanyLinkingEmail(
        email: String,
        token: VerificationToken,
        companyId: CompanyId,
        pendingData: PendingGuardianData
    ) {
        logger.info("Sending company linking email to $email for company ${companyId.value}")
        logger.info("Linking verification link: /api/auth/verify-email?token=${token.value}")
    }
}

// Extension for GuardianRepository
suspend fun GuardianRepository.findByCompanyAndEmail(
    companyId: CompanyId,
    email: String
): pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian? {
    val all = this.findAll(companyId, null, org.springframework.data.domain.Pageable.unpaged())
    return all.content.find { it.email?.lowercase() == email.lowercase() }
}