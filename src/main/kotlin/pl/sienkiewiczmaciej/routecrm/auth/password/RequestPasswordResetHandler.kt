package pl.sienkiewiczmaciej.routecrm.auth.password

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianRepository
import pl.sienkiewiczmaciej.routecrm.auth.global.PasswordResetToken
import pl.sienkiewiczmaciej.routecrm.auth.global.PasswordResetTokenRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class RequestPasswordResetCommand(
    val email: String,
    val companyId: CompanyId
)

data class RequestPasswordResetResult(
    val emailSent: Boolean,
    val email: String
)

@Component
class RequestPasswordResetHandler(
    private val globalGuardianRepository: GlobalGuardianRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailService: PasswordResetEmailService
) {
    @Transactional
    suspend fun handle(command: RequestPasswordResetCommand): RequestPasswordResetResult {
        val globalGuardian = globalGuardianRepository.findByEmail(command.email)

        if (globalGuardian != null) {
            passwordResetTokenRepository.deleteByGuardian(globalGuardian.id)

            val resetToken = PasswordResetToken.create(globalGuardian.id)
            passwordResetTokenRepository.save(resetToken)

            emailService.sendPasswordResetEmail(
                email = globalGuardian.email,
                token = resetToken.token,
                companyId = command.companyId
            )
        }

        return RequestPasswordResetResult(
            emailSent = true,
            email = command.email
        )
    }
}