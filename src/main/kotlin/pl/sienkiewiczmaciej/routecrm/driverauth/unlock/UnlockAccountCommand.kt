package pl.sienkiewiczmaciej.routecrm.driverauth.unlock

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class UnlockAccountCommand(
    val companyId: CompanyId,
    val driverId: DriverId
)