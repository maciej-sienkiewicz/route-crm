package pl.sienkiewiczmaciej.routecrm.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.auth.global.VerificationToken
import pl.sienkiewiczmaciej.routecrm.auth.login.LoginGuardianCommand
import pl.sienkiewiczmaciej.routecrm.auth.login.LoginGuardianHandler
import pl.sienkiewiczmaciej.routecrm.auth.password.RequestPasswordResetCommand
import pl.sienkiewiczmaciej.routecrm.auth.password.RequestPasswordResetHandler
import pl.sienkiewiczmaciej.routecrm.auth.password.ResetPasswordCommand
import pl.sienkiewiczmaciej.routecrm.auth.password.ResetPasswordHandler
import pl.sienkiewiczmaciej.routecrm.auth.register.*
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole

@RestController
@RequestMapping("/api/auth/guardian")
class GuardianAuthController(
    private val registerHandler: RegisterGuardianHandler,
    private val verifyEmailHandler: VerifyEmailHandler,
    private val loginHandler: LoginGuardianHandler,
    private val requestPasswordResetHandler: RequestPasswordResetHandler,
    private val resetPasswordHandler: ResetPasswordHandler,
    private val domainService: CompanyDomainService
) : BaseController() {

    @PostMapping("/register")
    suspend fun register(
        @Valid @RequestBody request: GuardianRegisterRequest,
        @RequestHeader("Host") host: String,
        httpRequest: HttpServletRequest
    ): ResponseEntity<GuardianRegisterResponse> {
        val companyId = domainService.getCompanyIdFromHost(host)

        val command = RegisterGuardianCommand(
            companyId = companyId,
            email = request.email,
            password = request.password,
            firstName = request.firstName,
            lastName = request.lastName,
            phone = request.phone,
            address = request.address?.let {
                Address(
                    street = it.street,
                    houseNumber = it.houseNumber,
                    apartmentNumber = it.apartmentNumber,
                    postalCode = it.postalCode,
                    city = it.city
                )
            }
        )

        return when (val result = registerHandler.handle(command)) {
            is RegisterGuardianResult.NewGuardianCreated -> {
                ResponseEntity.ok(
                    GuardianRegisterResponse(
                        success = true,
                        message = "Registration successful. Please check your email to verify your account.",
                        requiresEmailVerification = result.requiresEmailVerification
                    )
                )
            }
            is RegisterGuardianResult.ExistingGuardianPendingVerification -> {
                ResponseEntity.ok(
                    GuardianRegisterResponse(
                        success = true,
                        message = "We've sent a verification email to link your account to this company.",
                        requiresEmailVerification = true
                    )
                )
            }
        }
    }

    @GetMapping("/verify-email")
    suspend fun verifyEmail(
        @RequestParam token: String,
        @RequestHeader("Host") host: String
    ): ResponseEntity<VerifyEmailResponse> {
        val companyId = domainService.getCompanyIdFromHost(host)

        val command = VerifyEmailCommand(
            token = VerificationToken(token),
            companyId = companyId,
            pendingData = null
        )

        val result = verifyEmailHandler.handle(command)

        return ResponseEntity.ok(
            VerifyEmailResponse(
                success = true,
                message = "Email verified successfully",
                profileCreated = result.profileCreated
            )
        )
    }

    @PostMapping("/login")
    suspend fun login(
        @Valid @RequestBody request: GuardianLoginRequest,
        @RequestHeader("Host") host: String,
        httpRequest: HttpServletRequest
    ): ResponseEntity<GuardianLoginResponse> {
        val companyId = domainService.getCompanyIdFromHost(host)

        val command = LoginGuardianCommand(
            companyId = companyId,
            email = request.email,
            password = request.password
        )

        val result = loginHandler.handle(command)

        val principal = UserPrincipal(
            userId = UserId.generate(),
            companyId = result.companyId,
            email = result.email,
            firstName = result.firstName,
            lastName = result.lastName,
            role = UserRole.GUARDIAN,
            guardianId = result.guardianId.value
        )

        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            emptyList()
        )
        SecurityContextHolder.getContext().authentication = authentication

        val session: HttpSession = httpRequest.getSession(true)
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext())

        return ResponseEntity.ok(
            GuardianLoginResponse(
                success = true,
                message = "Login successful",
                guardian = GuardianInfo(
                    id = result.guardianId.value,
                    email = result.email,
                    firstName = result.firstName,
                    lastName = result.lastName,
                    emailVerified = result.emailVerified
                )
            )
        )
    }

    @PostMapping("/request-password-reset")
    suspend fun requestPasswordReset(
        @Valid @RequestBody request: RequestPasswordResetRequest,
        @RequestHeader("Host") host: String
    ): ResponseEntity<PasswordResetResponse> {
        val companyId = domainService.getCompanyIdFromHost(host)

        val command = RequestPasswordResetCommand(
            email = request.email,
            companyId = companyId
        )

        requestPasswordResetHandler.handle(command)

        return ResponseEntity.ok(
            PasswordResetResponse(
                success = true,
                message = "If an account exists with this email, a password reset link has been sent."
            )
        )
    }

    @PostMapping("/reset-password")
    suspend fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<PasswordResetResponse> {
        val command = ResetPasswordCommand(
            token = VerificationToken(request.token),
            newPassword = request.newPassword
        )

        resetPasswordHandler.handle(command)

        return ResponseEntity.ok(
            PasswordResetResponse(
                success = true,
                message = "Password reset successful. You can now login with your new password."
            )
        )
    }
}

data class GuardianRegisterRequest(
    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    @field:NotBlank(message = "First name is required")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    val lastName: String,

    @field:NotBlank(message = "Phone is required")
    val phone: String,

    val address: AddressRequest?
)

data class AddressRequest(
    val street: String,
    val houseNumber: String,
    val apartmentNumber: String?,
    val postalCode: String,
    val city: String
)

data class GuardianRegisterResponse(
    val success: Boolean,
    val message: String,
    val requiresEmailVerification: Boolean
)

data class VerifyEmailResponse(
    val success: Boolean,
    val message: String,
    val profileCreated: Boolean
)

data class GuardianLoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)

data class GuardianLoginResponse(
    val success: Boolean,
    val message: String,
    val guardian: GuardianInfo?
)

data class GuardianInfo(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val emailVerified: Boolean
)

data class RequestPasswordResetRequest(
    @field:Email
    @field:NotBlank
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank
    val token: String,

    @field:NotBlank
    @field:Size(min = 8)
    val newPassword: String
)

data class PasswordResetResponse(
    val success: Boolean,
    val message: String
)