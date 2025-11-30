package pl.sienkiewiczmaciej.routecrm.driverauth.login

data class LoginDriverCommand(
    val phoneNumber: String,
    val password: String
)