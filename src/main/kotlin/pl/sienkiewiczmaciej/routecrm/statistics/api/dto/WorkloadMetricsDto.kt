package pl.sienkiewiczmaciej.routecrm.statistics.api.dto

import java.time.LocalDate

data class WorkloadMetricsDto(
    val metricDate: LocalDate,
    val avgStopsPerVehicle: Double,
    val minStopsPerVehicle: Int,
    val maxStopsPerVehicle: Int,
    val stdDevStops: Double,
    val avgStopsPerDriver: Double,
    val minStopsPerDriver: Int,
    val maxStopsPerDriver: Int
)