package pl.sienkiewiczmaciej.routecrm.driverauth.changepassword

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class ChangePasswordCommand(
    val companyId: CompanyId,
    val driverId: DriverId,
    val oldPassword: String,
    val newPassword: String
)