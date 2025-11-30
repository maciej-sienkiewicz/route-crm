package pl.sienkiewiczmaciej.routecrm.driverauth.login

import java.time.Instant

sealed class LoginDriverResult {
    data class Success(
        val driverId: String,
        val companyId: String,
        val firstName: String,
        val lastName: String
    ) : LoginDriverResult()

    data class InvalidCredentials(val remainingAttempts: Int) : LoginDriverResult()

    data class AccountLocked(val lockedUntil: Instant?) : LoginDriverResult()

    object RequiresActivation : LoginDriverResult()

    object AccountSuspended : LoginDriverResult()

    object DriverNotFound : LoginDriverResult()
}