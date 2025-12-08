package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class DailyWorkloadMetrics(
    val companyId: CompanyId,
    val metricDate: LocalDate,
    val avgStopsPerVehicle: Double,
    val minStopsPerVehicle: Int,
    val maxStopsPerVehicle: Int,
    val stdDevStops: Double,
    val avgStopsPerDriver: Double,
    val minStopsPerDriver: Int,
    val maxStopsPerDriver: Int
)