package pl.sienkiewiczmaciej.routecrm.driverauth.domain

import java.time.Instant

sealed class LoginResult {
    object Success : LoginResult()

    data class InvalidCredentials(val remainingAttempts: Int) : LoginResult()

    data class AccountLocked(val lockedUntil: Instant?) : LoginResult()

    object RequiresActivation : LoginResult()

    object AccountSuspended : LoginResult()
}