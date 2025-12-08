package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class DailyFleetMetrics(
    val companyId: CompanyId,
    val metricDate: LocalDate,
    val totalVehicles: Int,
    val vehiclesInRoutes: Int,
    val vehiclesAvailable: Int,
    val totalDrivers: Int,
    val driversInRoutes: Int,
    val driversAvailable: Int,
    val routesWithoutDriver: Int
)