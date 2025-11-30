package pl.sienkiewiczmaciej.routecrm.driverauth

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driverauth.activate.ActivateAccountCommand
import pl.sienkiewiczmaciej.routecrm.driverauth.activate.ActivateAccountHandler
import pl.sienkiewiczmaciej.routecrm.driverauth.activate.ActivateAccountResult
import pl.sienkiewiczmaciej.routecrm.driverauth.changepassword.ChangePasswordCommand
import pl.sienkiewiczmaciej.routecrm.driverauth.changepassword.ChangePasswordHandler
import pl.sienkiewiczmaciej.routecrm.driverauth.login.LoginDriverCommand
import pl.sienkiewiczmaciej.routecrm.driverauth.login.LoginDriverHandler
import pl.sienkiewiczmaciej.routecrm.driverauth.login.LoginDriverResult
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/driver-auth")
class DriverAuthController(
    private val loginHandler: LoginDriverHandler,
    private val activateHandler: ActivateAccountHandler,
    private val changePasswordHandler: ChangePasswordHandler
) : BaseController() {

    @PostMapping("/login")
    suspend fun login(
        @Valid @RequestBody request: DriverLoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<DriverLoginResponse> {
        val command = LoginDriverCommand(
            phoneNumber = request.phoneNumber,
            password = request.password
        )

        return when (val result = loginHandler.handle(command, httpRequest)) {
            is LoginDriverResult.Success -> ResponseEntity.ok(
                DriverLoginResponse(
                    success = true,
                    message = "Login successful",
                    driver = DriverInfo(
                        id = result.driverId,
                        companyId = result.companyId,
                        firstName = result.firstName,
                        lastName = result.lastName
                    )
                )
            )

            is LoginDriverResult.InvalidCredentials -> ResponseEntity.status(401).body(
                DriverLoginResponse(
                    success = false,
                    message = "Invalid credentials. ${result.remainingAttempts} attempts remaining.",
                    error = "INVALID_CREDENTIALS",
                    remainingAttempts = result.remainingAttempts
                )
            )

            is LoginDriverResult.AccountLocked -> ResponseEntity.status(423).body(
                DriverLoginResponse(
                    success = false,
                    message = "Account is locked until ${result.lockedUntil}. Contact administrator.",
                    error = "ACCOUNT_LOCKED",
                    lockedUntil = result.lockedUntil?.toString()
                )
            )

            is LoginDriverResult.RequiresActivation -> ResponseEntity.status(403).body(
                DriverLoginResponse(
                    success = false,
                    message = "Account requires activation. Use /activate endpoint.",
                    error = "REQUIRES_ACTIVATION"
                )
            )

            is LoginDriverResult.AccountSuspended -> ResponseEntity.status(403).body(
                DriverLoginResponse(
                    success = false,
                    message = "Account is suspended. Contact administrator.",
                    error = "ACCOUNT_SUSPENDED"
                )
            )

            is LoginDriverResult.DriverNotFound -> ResponseEntity.status(404).body(
                DriverLoginResponse(
                    success = false,
                    message = "Driver not found",
                    error = "DRIVER_NOT_FOUND"
                )
            )
        }
    }

    @PostMapping("/activate")
    suspend fun activate(
        @Valid @RequestBody request: ActivateAccountRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ActivateAccountResponse> {
        val command = ActivateAccountCommand(
            phoneNumber = request.phoneNumber,
            activationPin = request.activationPin,
            newPassword = request.newPassword
        )
        val principal = getPrincipal()


        return when (val result = activateHandler.handle(principal, command, httpRequest)) {
            is ActivateAccountResult.Success -> ResponseEntity.ok(
                ActivateAccountResponse(
                    success = true,
                    message = "Account activated successfully. You are now logged in.",
                    driver = DriverInfo(
                        id = result.driverId,
                        companyId = result.companyId,
                        firstName = result.firstName,
                        lastName = result.lastName
                    )
                )
            )

            is ActivateAccountResult.InvalidPin -> ResponseEntity.status(401).body(
                ActivateAccountResponse(
                    success = false,
                    message = "Invalid activation PIN",
                    error = "INVALID_PIN"
                )
            )

            is ActivateAccountResult.AccountNotFound -> ResponseEntity.status(404).body(
                ActivateAccountResponse(
                    success = false,
                    message = "Account not found",
                    error = "ACCOUNT_NOT_FOUND"
                )
            )

            is ActivateAccountResult.AccountAlreadyActivated -> ResponseEntity.status(400).body(
                ActivateAccountResponse(
                    success = false,
                    message = "Account is already activated. Use /login endpoint.",
                    error = "ALREADY_ACTIVATED"
                )
            )

            is ActivateAccountResult.InvalidPassword -> ResponseEntity.status(400).body(
                ActivateAccountResponse(
                    success = false,
                    message = result.reason,
                    error = "INVALID_PASSWORD"
                )
            )
        }
    }

    @PostMapping("/change-password")
    suspend fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Map<String, String>> {
        val principal = getPrincipal()
        val command = ChangePasswordCommand(
            companyId = principal.companyId,
            driverId = DriverId(principal.driverId!!),
            oldPassword = request.oldPassword,
            newPassword = request.newPassword
        )

        changePasswordHandler.handle(principal, command)

        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }

    @GetMapping("/me")
    suspend fun getCurrentDriver(): ResponseEntity<DriverProfileResponse> {
        val principal = getPrincipal()

        return ResponseEntity.ok(
            DriverProfileResponse(
                id = principal.driverId!!,
                companyId = principal.companyId.value,
                firstName = principal.firstName,
                lastName = principal.lastName,
                email = principal.email
            )
        )
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        request.session.invalidate()
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}

data class DriverLoginRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    val phoneNumber: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class DriverLoginResponse(
    val success: Boolean,
    val message: String,
    val driver: DriverInfo? = null,
    val error: String? = null,
    val remainingAttempts: Int? = null,
    val lockedUntil: String? = null
)

data class ActivateAccountRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    val phoneNumber: String,

    @field:NotBlank(message = "Activation PIN is required")
    @field:Size(min = 6, max = 6, message = "Activation PIN must be exactly 6 characters")
    val activationPin: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val newPassword: String
)

data class ActivateAccountResponse(
    val success: Boolean,
    val message: String,
    val driver: DriverInfo? = null,
    val error: String? = null
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Old password is required")
    val oldPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val newPassword: String
)

data class DriverInfo(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String
)

data class DriverProfileResponse(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String,
    val email: String?
)