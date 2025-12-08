package pl.sienkiewiczmaciej.routecrm.statistics.api.dto

import java.time.LocalDate

data class CapacityMetricsDto(
    val metricDate: LocalDate,
    val avgSaturationPercent: Double,
    val minSaturationPercent: Double,
    val maxSaturationPercent: Double,
    val totalRoutes: Int,
    val routesLowSaturation: Int
)