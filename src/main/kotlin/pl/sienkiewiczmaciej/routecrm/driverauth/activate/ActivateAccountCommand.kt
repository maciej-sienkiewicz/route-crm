package pl.sienkiewiczmaciej.routecrm.driverauth.activate

data class ActivateAccountCommand(
    val phoneNumber: String,
    val activationPin: String,
    val newPassword: String
)