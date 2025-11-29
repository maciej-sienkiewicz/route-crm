// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/auth/global/GlobalGuardianDomainModels.kt
package pl.sienkiewiczmaciej.routecrm.auth.global

import org.springframework.security.crypto.password.PasswordEncoder
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import java.time.Instant
import java.util.*

@JvmInline
value class GlobalGuardianId(val value: String) {
    companion object {
        fun generate() = GlobalGuardianId("GG-${UUID.randomUUID()}")
        fun from(value: String) = GlobalGuardianId(value)
    }
}

@JvmInline
value class VerificationToken(val value: String) {
    companion object {
        fun generate() = VerificationToken(UUID.randomUUID().toString())
    }
}

data class GlobalGuardian(
    val id: GlobalGuardianId,
    val email: String,
    val passwordHash: String,
    val emailVerified: Boolean = false,
    val phone: String? = null,
    val phoneVerified: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val lastPasswordChange: Instant = Instant.now(),
    val accountLocked: Boolean = false,
    val failedLoginAttempts: Int = 0,
    val lastFailedLogin: Instant? = null
) {
    companion object {
        fun create(
            email: String,
            password: String,
            passwordEncoder: PasswordEncoder,
            phone: String? = null
        ): GlobalGuardian {
            require(email.isNotBlank()) { "Email is required" }
            require(email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                "Invalid email format"
            }
            require(password.length >= 8) { "Password must be at least 8 characters" }

            return GlobalGuardian(
                id = GlobalGuardianId.generate(),
                email = email.lowercase().trim(),
                passwordHash = passwordEncoder.encode(password),
                phone = phone?.trim(),
                emailVerified = false,
                phoneVerified = false
            )
        }

        fun generateRandomPassword(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
            return (1..12)
                .map { chars.random() }
                .joinToString("")
        }
    }

    fun verifyEmail(): GlobalGuardian {
        return copy(
            emailVerified = true,
            updatedAt = Instant.now()
        )
    }

    fun changePassword(newPassword: String, passwordEncoder: PasswordEncoder): GlobalGuardian {
        require(newPassword.length >= 8) { "Password must be at least 8 characters" }

        return copy(
            passwordHash = passwordEncoder.encode(newPassword),
            lastPasswordChange = Instant.now(),
            updatedAt = Instant.now(),
            failedLoginAttempts = 0,
            accountLocked = false
        )
    }

    fun changeEmail(newEmail: String): GlobalGuardian {
        require(newEmail.isNotBlank()) { "Email is required" }
        require(newEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            "Invalid email format"
        }

        return copy(
            email = newEmail.lowercase().trim(),
            emailVerified = false,
            updatedAt = Instant.now()
        )
    }

    fun recordFailedLogin(): GlobalGuardian {
        val newAttempts = failedLoginAttempts + 1
        return copy(
            failedLoginAttempts = newAttempts,
            lastFailedLogin = Instant.now(),
            accountLocked = newAttempts >= 5,
            updatedAt = Instant.now()
        )
    }

    fun resetFailedLogins(): GlobalGuardian {
        return copy(
            failedLoginAttempts = 0,
            lastFailedLogin = null,
            updatedAt = Instant.now()
        )
    }

    fun unlock(): GlobalGuardian {
        return copy(
            accountLocked = false,
            failedLoginAttempts = 0,
            updatedAt = Instant.now()
        )
    }

    fun verifyPassword(password: String, passwordEncoder: PasswordEncoder): Boolean {
        return passwordEncoder.matches(password, passwordHash)
    }
}

data class EmailVerification(
    val id: String = UUID.randomUUID().toString(),
    val globalGuardianId: GlobalGuardianId,
    val email: String,
    val token: VerificationToken,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    val verifiedAt: Instant? = null
) {
    companion object {
        fun create(globalGuardianId: GlobalGuardianId, email: String): EmailVerification {
            return EmailVerification(
                globalGuardianId = globalGuardianId,
                email = email,
                token = VerificationToken.generate(),
                expiresAt = Instant.now().plusSeconds(86400)
            )
        }
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun isVerified(): Boolean = verifiedAt != null

    fun verify(): EmailVerification {
        require(!isExpired()) { "Verification token has expired" }
        require(!isVerified()) { "Email already verified" }

        return copy(verifiedAt = Instant.now())
    }
}

data class PasswordResetToken(
    val id: String = UUID.randomUUID().toString(),
    val globalGuardianId: GlobalGuardianId,
    val token: VerificationToken,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now(),
    val usedAt: Instant? = null
) {
    companion object {
        fun create(globalGuardianId: GlobalGuardianId): PasswordResetToken {
            return PasswordResetToken(
                globalGuardianId = globalGuardianId,
                token = VerificationToken.generate(),
                expiresAt = Instant.now().plusSeconds(3600)
            )
        }
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun isUsed(): Boolean = usedAt != null

    fun use(): PasswordResetToken {
        require(!isExpired()) { "Reset token has expired" }
        require(!isUsed()) { "Reset token already used" }

        return copy(usedAt = Instant.now())
    }
}

data class CompanyProfile(
    val guardianId: GuardianId,
    val companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId,
    val globalGuardianId: GlobalGuardianId,
    val accountStatus: AccountStatus,
    val lastLogin: Instant?,
    val loginCount: Int
)

enum class AccountStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    PENDING_VERIFICATION
}

interface GlobalGuardianRepository {
    suspend fun save(guardian: GlobalGuardian): GlobalGuardian
    suspend fun findById(id: GlobalGuardianId): GlobalGuardian?
    suspend fun findByEmail(email: String): GlobalGuardian?
    suspend fun existsByEmail(email: String): Boolean
    suspend fun delete(id: GlobalGuardianId)
}

interface EmailVerificationRepository {
    suspend fun save(verification: EmailVerification): EmailVerification
    suspend fun findByToken(token: VerificationToken): EmailVerification?
    suspend fun findActiveByGuardian(guardianId: GlobalGuardianId): EmailVerification?
    suspend fun deleteByGuardian(guardianId: GlobalGuardianId)
}

interface PasswordResetTokenRepository {
    suspend fun save(token: PasswordResetToken): PasswordResetToken
    suspend fun findByToken(token: VerificationToken): PasswordResetToken?
    suspend fun findActiveByGuardian(guardianId: GlobalGuardianId): PasswordResetToken?
    suspend fun deleteByGuardian(guardianId: GlobalGuardianId)
}

data class CompanyDomain(
    val companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId,
    val domain: String,
    val subdomain: String
) {
    fun fullDomain(): String = "$subdomain.$domain"
}

interface CompanyDomainRepository {
    suspend fun findByDomain(domain: String): CompanyDomain?
    suspend fun findByCompanyId(companyId: pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId): CompanyDomain?
}