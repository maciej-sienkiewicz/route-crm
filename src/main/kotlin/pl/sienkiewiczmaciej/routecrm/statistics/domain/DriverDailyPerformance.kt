package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class DriverDailyPerformance(
    val companyId: CompanyId,
    val driverId: DriverId,
    val metricDate: LocalDate,
    val totalStops: Int,
    val ontimeStops: Int,
    val lateStops: Int,
    val otdPercentage: Double,
    val avgDelayMinutes: Int,
    val maxDelayMinutes: Int
)