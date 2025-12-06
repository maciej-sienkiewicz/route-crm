package pl.sienkiewiczmaciej.routecrm.driverauth.login

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsRepository
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.LoginResult
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole

@Component
class LoginDriverHandler(
    private val credentialsRepository: DriverCredentialsRepository,
    private val driverRepository: DriverRepository,
    private val passwordEncoder: PasswordEncoder
) {
    suspend fun handle(
        command: LoginDriverCommand,
        httpRequest: HttpServletRequest
    ): LoginDriverResult {
        val allCredentials = credentialsRepository.findByPhoneNumber(
            phoneNumber = command.phoneNumber
        )

        if (allCredentials == null) {
            val foundInAnyCompany = tryFindInAllCompanies(command.phoneNumber)
            if (foundInAnyCompany == null) {
                return LoginDriverResult.InvalidCredentials(3)
            }
        }

        val credentials = allCredentials ?: tryFindInAllCompanies(command.phoneNumber)
        ?: return LoginDriverResult.InvalidCredentials(3)

        val passwordMatches = passwordEncoder.matches(command.password, credentials.passwordHash)
        val (updatedCredentials, result) = credentials.attemptLogin(command.password, passwordMatches)

        credentialsRepository.save(updatedCredentials)

        return when (result) {
            is LoginResult.Success -> {
                val driver = driverRepository.findById(credentials.companyId, credentials.driverId)
                    ?: return LoginDriverResult.DriverNotFound

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

                LoginDriverResult.Success(
                    driverId = driver.id.value,
                    companyId = driver.companyId.value,
                    firstName = driver.firstName,
                    lastName = driver.lastName
                )
            }

            is LoginResult.InvalidCredentials -> {
                LoginDriverResult.InvalidCredentials(result.remainingAttempts)
            }

            is LoginResult.AccountLocked -> {
                LoginDriverResult.AccountLocked(result.lockedUntil)
            }

            is LoginResult.RequiresActivation -> {
                LoginDriverResult.RequiresActivation
            }

            is LoginResult.AccountSuspended -> {
                LoginDriverResult.AccountSuspended
            }
        }
    }

    private suspend fun tryFindInAllCompanies(phoneNumber: String): pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentials? {
        return null
    }
}