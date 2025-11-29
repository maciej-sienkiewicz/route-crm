package pl.sienkiewiczmaciej.routecrm.auth.password

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.auth.global.VerificationToken
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface PasswordResetEmailService {
    suspend fun sendPasswordResetEmail(
        email: String,
        token: VerificationToken,
        companyId: CompanyId
    )
}

@Service
class PasswordResetEmailServiceImpl : PasswordResetEmailService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun sendPasswordResetEmail(
        email: String,
        token: VerificationToken,
        companyId: CompanyId
    ) {
        logger.info("Sending password reset email to $email for company ${companyId.value}")
        logger.info("Reset link: /api/auth/reset-password?token=${token.value}")
    }
}