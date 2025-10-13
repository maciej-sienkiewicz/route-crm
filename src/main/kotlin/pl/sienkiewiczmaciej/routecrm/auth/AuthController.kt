package pl.sienkiewiczmaciej.routecrm.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.UserJpaRepository

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpServletRequest: HttpServletRequest
    ): ResponseEntity<LoginResponse> {
        val user = userRepository.findByEmail(request.email)
            ?: return ResponseEntity.status(401).body(
                LoginResponse(success = false, message = "Invalid credentials")
            )

        if (!user.active) {
            return ResponseEntity.status(401).body(
                LoginResponse(success = false, message = "Account is inactive")
            )
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            return ResponseEntity.status(401).body(
                LoginResponse(success = false, message = "Invalid credentials")
            )
        }

        // Utwórz principal
        val principal = UserPrincipal(
            userId = UserId.from(user.id),
            companyId = CompanyId.from(user.companyId),
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role,
            guardianId = user.guardianId,
            driverId = user.driverId
        )

        // Ustaw authentication
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            emptyList()
        )
        SecurityContextHolder.getContext().authentication = authentication

        // Zapisz w sesji
        val session: HttpSession = httpServletRequest.getSession(true)
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext())

        return ResponseEntity.ok(
            LoginResponse(
                success = true,
                message = "Login successful",
                user = UserInfo(
                    id = user.id,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    role = user.role.name,
                    companyId = user.companyId
                )
            )
        )
    }

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<UserInfo> {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal as? UserPrincipal
            ?: return ResponseEntity.status(401).build()

        return ResponseEntity.ok(
            UserInfo(
                id = principal.userId.value,
                email = principal.email,
                firstName = principal.firstName,
                lastName = principal.lastName,
                role = principal.role.name,
                companyId = principal.companyId.value
            )
        )
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        request.session.invalidate()
        SecurityContextHolder.clearContext()
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserInfo? = null
)

data class UserInfo(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val companyId: String
)