package pl.sienkiewiczmaciej.routecrm.driverauth.activate

sealed class ActivateAccountResult {
    data class Success(
        val driverId: String,
        val companyId: String,
        val firstName: String,
        val lastName: String
    ) : ActivateAccountResult()

    object InvalidPin : ActivateAccountResult()

    object AccountNotFound : ActivateAccountResult()

    object AccountAlreadyActivated : ActivateAccountResult()

    data class InvalidPassword(val reason: String) : ActivateAccountResult()
}