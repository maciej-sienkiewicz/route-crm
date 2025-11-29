package pl.sienkiewiczmaciej.routecrm.auth.global.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardian
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianId
import java.time.Instant

@Entity
@Table(
    name = "global_guardians",
    indexes = [
        Index(name = "idx_global_guardians_email", columnList = "email", unique = true)
    ]
)
class GlobalGuardianEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(unique = true, nullable = false, length = 255)
    val email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    val passwordHash: String,

    @Column(name = "email_verified", nullable = false)
    val emailVerified: Boolean = false,

    @Column(length = 20)
    val phone: String? = null,

    @Column(name = "phone_verified", nullable = false)
    val phoneVerified: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @Column(name = "last_password_change", nullable = false)
    val lastPasswordChange: Instant = Instant.now(),

    @Column(name = "account_locked", nullable = false)
    val accountLocked: Boolean = false,

    @Column(name = "failed_login_attempts", nullable = false)
    val failedLoginAttempts: Int = 0,

    @Column(name = "last_failed_login")
    val lastFailedLogin: Instant? = null
) {
    fun toDomain() = GlobalGuardian(
        id = GlobalGuardianId(id),
        email = email,
        passwordHash = passwordHash,
        emailVerified = emailVerified,
        phone = phone,
        phoneVerified = phoneVerified,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastPasswordChange = lastPasswordChange,
        accountLocked = accountLocked,
        failedLoginAttempts = failedLoginAttempts,
        lastFailedLogin = lastFailedLogin
    )

    companion object {
        fun fromDomain(guardian: GlobalGuardian) = GlobalGuardianEntity(
            id = guardian.id.value,
            email = guardian.email,
            passwordHash = guardian.passwordHash,
            emailVerified = guardian.emailVerified,
            phone = guardian.phone,
            phoneVerified = guardian.phoneVerified,
            createdAt = guardian.createdAt,
            updatedAt = guardian.updatedAt,
            lastPasswordChange = guardian.lastPasswordChange,
            accountLocked = guardian.accountLocked,
            failedLoginAttempts = guardian.failedLoginAttempts,
            lastFailedLogin = guardian.lastFailedLogin
        )
    }
}

@Entity
@Table(
    name = "email_verifications",
    indexes = [
        Index(name = "idx_email_verifications_token", columnList = "token", unique = true),
        Index(name = "idx_email_verifications_guardian", columnList = "global_guardian_id")
    ]
)
class EmailVerificationEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "global_guardian_id", nullable = false, length = 50)
    val globalGuardianId: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(unique = true, nullable = false, length = 50)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "verified_at")
    val verifiedAt: Instant? = null
) {
    fun toDomain() = pl.sienkiewiczmaciej.routecrm.auth.global.EmailVerification(
        id = id,
        globalGuardianId = GlobalGuardianId(globalGuardianId),
        email = email,
        token = pl.sienkiewiczmaciej.routecrm.auth.global.VerificationToken(token),
        expiresAt = expiresAt,
        createdAt = createdAt,
        verifiedAt = verifiedAt
    )

    companion object {
        fun fromDomain(verification: pl.sienkiewiczmaciej.routecrm.auth.global.EmailVerification) =
            EmailVerificationEntity(
                id = verification.id,
                globalGuardianId = verification.globalGuardianId.value,
                email = verification.email,
                token = verification.token.value,
                expiresAt = verification.expiresAt,
                createdAt = verification.createdAt,
                verifiedAt = verification.verifiedAt
            )
    }
}

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = [
        Index(name = "idx_password_reset_token", columnList = "token", unique = true),
        Index(name = "idx_password_reset_guardian", columnList = "global_guardian_id")
    ]
)
class PasswordResetTokenEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "global_guardian_id", nullable = false, length = 50)
    val globalGuardianId: String,

    @Column(unique = true, nullable = false, length = 50)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "used_at")
    val usedAt: Instant? = null
) {
    fun toDomain() = pl.sienkiewiczmaciej.routecrm.auth.global.PasswordResetToken(
        id = id,
        globalGuardianId = GlobalGuardianId(globalGuardianId),
        token = pl.sienkiewiczmaciej.routecrm.auth.global.VerificationToken(token),
        expiresAt = expiresAt,
        createdAt = createdAt,
        usedAt = usedAt
    )

    companion object {
        fun fromDomain(token: pl.sienkiewiczmaciej.routecrm.auth.global.PasswordResetToken) =
            PasswordResetTokenEntity(
                id = token.id,
                globalGuardianId = token.globalGuardianId.value,
                token = token.token.value,
                expiresAt = token.expiresAt,
                createdAt = token.createdAt,
                usedAt = token.usedAt
            )
    }
}