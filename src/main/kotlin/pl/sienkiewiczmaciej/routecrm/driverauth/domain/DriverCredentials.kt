package pl.sienkiewiczmaciej.routecrm.driverauth.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.temporal.ChronoUnit

data class DriverCredentials(
    val id: DriverCredentialsId,
    val driverId: DriverId,
    val companyId: CompanyId,
    val phoneNumber: String,
    val passwordHash: String,
    val accountStatus: AccountStatus,
    val failedLoginAttempts: Int,
    val lastFailedLoginAt: Instant?,
    val lockedAt: Instant?,
    val lockedUntil: Instant?,
    val activationPin: String?,
    val activatedAt: Instant?,
    val passwordChangedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        const val MAX_FAILED_ATTEMPTS = 3
        const val LOCK_DURATION_HOURS = 24L
        const val ACTIVATION_PIN_LENGTH = 6

        fun createForNewDriver(
            driverId: DriverId,
            companyId: CompanyId,
            phoneNumber: String,
            activationPin: String,
            hashedPin: String
        ): DriverCredentials {
            require(phoneNumber.isNotBlank()) { "Phone number is required" }
            require(phoneNumber.matches(Regex("^\\+?[1-9]\\d{1,14}$"))) {
                "Invalid phone number format"
            }
            require(activationPin.length == ACTIVATION_PIN_LENGTH) {
                "Activation PIN must be exactly $ACTIVATION_PIN_LENGTH characters"
            }

            return DriverCredentials(
                id = DriverCredentialsId.generate(),
                driverId = driverId,
                companyId = companyId,
                phoneNumber = phoneNumber.trim(),
                passwordHash = hashedPin,
                accountStatus = AccountStatus.PENDING_ACTIVATION,
                failedLoginAttempts = 0,
                lastFailedLoginAt = null,
                lockedAt = null,
                lockedUntil = null,
                activationPin = activationPin,
                activatedAt = null,
                passwordChangedAt = Instant.now(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
    }

    fun attemptLogin(providedPassword: String, passwordMatches: Boolean): Pair<DriverCredentials, LoginResult> {
        return when {
            accountStatus == AccountStatus.SUSPENDED -> {
                this to LoginResult.AccountSuspended
            }

            accountStatus == AccountStatus.LOCKED && isStillLocked() -> {
                this to LoginResult.AccountLocked(lockedUntil)
            }

            accountStatus == AccountStatus.LOCKED && !isStillLocked() -> {
                val unlocked = autoUnlock()
                unlocked.attemptLogin(providedPassword, passwordMatches)
            }

            accountStatus == AccountStatus.PENDING_ACTIVATION -> {
                this to LoginResult.RequiresActivation
            }

            !passwordMatches -> {
                val updated = incrementFailedAttempts()
                if (updated.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                    val locked = updated.lock()
                    locked to LoginResult.AccountLocked(locked.lockedUntil)
                } else {
                    updated to LoginResult.InvalidCredentials(
                        remainingAttempts = MAX_FAILED_ATTEMPTS - updated.failedLoginAttempts
                    )
                }
            }

            else -> {
                val updated = resetFailedAttempts()
                updated to LoginResult.Success
            }
        }
    }

    fun activate(newPassword: String, newPasswordHash: String): DriverCredentials {
        require(accountStatus == AccountStatus.PENDING_ACTIVATION) {
            "Only accounts in PENDING_ACTIVATION status can be activated"
        }
        require(newPassword.length >= 8) {
            "Password must be at least 8 characters long"
        }
        require(newPassword.matches(Regex(".*[A-Z].*"))) {
            "Password must contain at least one uppercase letter"
        }
        require(newPassword.matches(Regex(".*[a-z].*"))) {
            "Password must contain at least one lowercase letter"
        }
        require(newPassword.matches(Regex(".*\\d.*"))) {
            "Password must contain at least one digit"
        }

        return copy(
            passwordHash = newPasswordHash,
            accountStatus = AccountStatus.ACTIVE,
            activationPin = null,
            activatedAt = Instant.now(),
            failedLoginAttempts = 0,
            passwordChangedAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    fun changePassword(oldPassword: String, newPassword: String, oldPasswordMatches: Boolean, newPasswordHash: String): DriverCredentials {
        require(accountStatus == AccountStatus.ACTIVE) {
            "Only active accounts can change password"
        }
        require(oldPasswordMatches) {
            "Old password is incorrect"
        }
        require(newPassword.length >= 8) {
            "Password must be at least 8 characters long"
        }
        require(newPassword != oldPassword) {
            "New password must be different from old password"
        }

        return copy(
            passwordHash = newPasswordHash,
            passwordChangedAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    fun unlock(): DriverCredentials {
        require(accountStatus == AccountStatus.LOCKED) {
            "Only locked accounts can be unlocked"
        }

        return copy(
            accountStatus = AccountStatus.ACTIVE,
            failedLoginAttempts = 0,
            lockedAt = null,
            lockedUntil = null,
            lastFailedLoginAt = null,
            updatedAt = Instant.now()
        )
    }

    fun suspend(): DriverCredentials {
        require(accountStatus in listOf(AccountStatus.ACTIVE, AccountStatus.LOCKED)) {
            "Only active or locked accounts can be suspended"
        }

        return copy(
            accountStatus = AccountStatus.SUSPENDED,
            updatedAt = Instant.now()
        )
    }

    fun unsuspend(): DriverCredentials {
        require(accountStatus == AccountStatus.SUSPENDED) {
            "Only suspended accounts can be unsuspended"
        }

        return copy(
            accountStatus = AccountStatus.ACTIVE,
            failedLoginAttempts = 0,
            updatedAt = Instant.now()
        )
    }

    fun resetPassword(newPin: String, hashedPin: String): DriverCredentials {
        require(newPin.length == ACTIVATION_PIN_LENGTH) {
            "Reset PIN must be exactly $ACTIVATION_PIN_LENGTH characters"
        }

        return copy(
            passwordHash = hashedPin,
            accountStatus = AccountStatus.PENDING_ACTIVATION,
            activationPin = newPin,
            failedLoginAttempts = 0,
            lockedAt = null,
            lockedUntil = null,
            passwordChangedAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun incrementFailedAttempts(): DriverCredentials {
        return copy(
            failedLoginAttempts = failedLoginAttempts + 1,
            lastFailedLoginAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun resetFailedAttempts(): DriverCredentials {
        return copy(
            failedLoginAttempts = 0,
            lastFailedLoginAt = null,
            updatedAt = Instant.now()
        )
    }

    private fun lock(): DriverCredentials {
        val now = Instant.now()
        return copy(
            accountStatus = AccountStatus.LOCKED,
            lockedAt = now,
            lockedUntil = now.plus(LOCK_DURATION_HOURS, ChronoUnit.HOURS),
            updatedAt = now
        )
    }

    private fun autoUnlock(): DriverCredentials {
        return copy(
            accountStatus = AccountStatus.ACTIVE,
            failedLoginAttempts = 0,
            lockedAt = null,
            lockedUntil = null,
            updatedAt = Instant.now()
        )
    }

    private fun isStillLocked(): Boolean {
        return lockedUntil?.isAfter(Instant.now()) ?: false
    }

    fun canLogin(): Boolean {
        return when (accountStatus) {
            AccountStatus.ACTIVE -> true
            AccountStatus.LOCKED -> !isStillLocked()
            AccountStatus.PENDING_ACTIVATION, AccountStatus.SUSPENDED -> false
        }
    }
}