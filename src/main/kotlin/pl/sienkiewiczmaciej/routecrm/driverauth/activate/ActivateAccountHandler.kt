package pl.sienkiewiczmaciej.routecrm.driverauth.activate

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.AccountStatus
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole

@Component
class ActivateAccountHandler(
    private val credentialsRepository: DriverCredentialsRepository,
    private val driverRepository: DriverRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    suspend fun handle(
        command: ActivateAccountCommand,
        httpRequest: HttpServletRequest
    ): ActivateAccountResult {
        val credentials = findCredentialsByPhoneInAnyCompany(command.phoneNumber)
            ?: return ActivateAccountResult.AccountNotFound

        if(credentials.accountStatus != AccountStatus.PENDING_ACTIVATION) {
                return ActivateAccountResult.AccountAlreadyActivated
            }

        if (credentials.activationPin != command.activationPin) {
            return ActivateAccountResult.InvalidPin
        }

        return try {
            val newPasswordHash = passwordEncoder.encode(command.newPassword)
            val activated = credentials.activate(command.newPassword, newPasswordHash)
            credentialsRepository.save(activated)

            val driver = driverRepository.findById(credentials.companyId, credentials.driverId)
                ?: return ActivateAccountResult.AccountNotFound

            val principal = UserPrincipal(
                userId = UserId("DRVUSR-${driver.id.value}"),
                companyId = driver.companyId,
                role = UserRole.DRIVER,
                email = driver.email,
                firstName = driver.firstName,
                lastName = driver.lastName,
                guardianId = null,
                driverId = driver.id.value
            )

            val authentication = UsernamePasswordAuthenticationToken(
                principal,
                null,
                listOf(SimpleGrantedAuthority("ROLE_DRIVER"))
            )
            SecurityContextHolder.getContext().authentication = authentication

            val session: HttpSession = httpRequest.getSession(true)
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext())

            ActivateAccountResult.Success(
                driverId = driver.id.value,
                companyId = driver.companyId.value,
                firstName = driver.firstName,
                lastName = driver.lastName
            )
        } catch (e: IllegalArgumentException) {
            ActivateAccountResult.InvalidPassword(e.message ?: "Invalid password")
        }
    }

    private suspend fun findCredentialsByPhoneInAnyCompany(
        phoneNumber: String
    ): pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentials? {
        return credentialsRepository.findByPhoneNumber(
            phoneNumber
        )
    }
}