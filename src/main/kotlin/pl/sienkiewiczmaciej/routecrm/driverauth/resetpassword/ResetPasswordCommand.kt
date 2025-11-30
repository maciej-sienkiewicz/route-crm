package pl.sienkiewiczmaciej.routecrm.driverauth.resetpassword

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class ResetPasswordCommand(
    val companyId: CompanyId,
    val driverId: DriverId
)

data class ResetPasswordResult(
    val newPin: String
)