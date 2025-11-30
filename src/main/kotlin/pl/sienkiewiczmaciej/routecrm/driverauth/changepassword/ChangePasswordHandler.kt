package pl.sienkiewiczmaciej.routecrm.driverauth.changepassword

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsRepository
import pl.sienkiewiczmaciej.routecrm.driverauth.service.DriverCredentialsNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

@Component
class ChangePasswordHandler(
    private val credentialsRepository: DriverCredentialsRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: ChangePasswordCommand) {
        authService.requireRole(principal, UserRole.DRIVER)
        authService.requireSameCompany(principal.companyId, command.companyId)

        require(principal.driverId == command.driverId.value) {
            "Driver can only change their own password"
        }

        val credentials = credentialsRepository.findByDriverId(command.companyId, command.driverId)
            ?: throw DriverCredentialsNotFoundException(command.driverId)

        val oldPasswordMatches = passwordEncoder.matches(command.oldPassword, credentials.passwordHash)
        val newPasswordHash = passwordEncoder.encode(command.newPassword)

        val updated = credentials.changePassword(
            oldPassword = command.oldPassword,
            newPassword = command.newPassword,
            oldPasswordMatches = oldPasswordMatches,
            newPasswordHash = newPasswordHash
        )

        credentialsRepository.save(updated)
    }
}