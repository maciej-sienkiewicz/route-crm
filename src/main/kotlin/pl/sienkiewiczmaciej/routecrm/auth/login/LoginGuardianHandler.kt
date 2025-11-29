package pl.sienkiewiczmaciej.routecrm.auth.login

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianId
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianRepository
import pl.sienkiewiczmaciej.routecrm.auth.register.findByCompanyAndEmail
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.UnauthorizedException

data class LoginGuardianCommand(
    val companyId: CompanyId,
    val email: String,
    val password: String
)

data class LoginGuardianResult(
    val globalGuardianId: GlobalGuardianId,
    val guardianId: GuardianId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val companyId: CompanyId,
    val emailVerified: Boolean
)

@Component
class LoginGuardianHandler(
    private val globalGuardianRepository: GlobalGuardianRepository,
    private val guardianRepository: GuardianRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    suspend fun handle(command: LoginGuardianCommand): LoginGuardianResult {
        val globalGuardian = globalGuardianRepository.findByEmail(command.email)
            ?: throw UnauthorizedException("Invalid credentials")

        if (globalGuardian.accountLocked) {
            throw UnauthorizedException("Account is locked due to too many failed login attempts")
        }

        if (!globalGuardian.verifyPassword(command.password, passwordEncoder)) {
            val updated = globalGuardian.recordFailedLogin()
            globalGuardianRepository.save(updated)
            throw UnauthorizedException("Invalid credentials")
        }

        val guardian = guardianRepository.findByCompanyAndEmail(command.companyId, command.email)
            ?: throw UnauthorizedException("No account in this company")

        val resetFailures = globalGuardian.resetFailedLogins()
        globalGuardianRepository.save(resetFailures)

        return LoginGuardianResult(
            globalGuardianId = globalGuardian.id,
            guardianId = guardian.id,
            email = globalGuardian.email,
            firstName = guardian.firstName,
            lastName = guardian.lastName,
            companyId = command.companyId,
            emailVerified = globalGuardian.emailVerified
        )
    }
}