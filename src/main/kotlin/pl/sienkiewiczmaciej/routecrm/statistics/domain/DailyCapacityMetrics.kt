package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class DailyCapacityMetrics(
    val companyId: CompanyId,
    val metricDate: LocalDate,
    val avgSaturationPercent: Double,
    val minSaturationPercent: Double,
    val maxSaturationPercent: Double,
    val totalRoutes: Int,
    val routesLowSaturation: Int
)