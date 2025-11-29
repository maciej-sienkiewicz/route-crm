package pl.sienkiewiczmaciej.routecrm.auth.password

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianRepository
import pl.sienkiewiczmaciej.routecrm.auth.global.PasswordResetTokenRepository
import pl.sienkiewiczmaciej.routecrm.auth.global.VerificationToken
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException

data class ResetPasswordCommand(
    val token: VerificationToken,
    val newPassword: String
)

data class ResetPasswordResult(
    val success: Boolean,
    val email: String
)

class InvalidResetTokenException : NotFoundException("Password reset token is invalid or expired")

@Component
class ResetPasswordHandler(
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val globalGuardianRepository: GlobalGuardianRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    suspend fun handle(command: ResetPasswordCommand): ResetPasswordResult {
        val resetToken = passwordResetTokenRepository.findByToken(command.token)
            ?: throw InvalidResetTokenException()

        require(!resetToken.isExpired()) { "Password reset token has expired" }
        require(!resetToken.isUsed()) { "Password reset token has already been used" }

        val globalGuardian = globalGuardianRepository.findById(resetToken.globalGuardianId)
            ?: throw NotFoundException("Guardian not found")

        val updatedGuardian = globalGuardian.changePassword(command.newPassword, passwordEncoder)
        globalGuardianRepository.save(updatedGuardian)

        val usedToken = resetToken.use()
        passwordResetTokenRepository.save(usedToken)

        return ResetPasswordResult(
            success = true,
            email = updatedGuardian.email
        )
    }
}