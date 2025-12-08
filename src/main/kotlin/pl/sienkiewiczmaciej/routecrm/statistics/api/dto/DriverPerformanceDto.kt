package pl.sienkiewiczmaciej.routecrm.statistics.api.dto

import java.time.LocalDate

data class DriverPerformanceDto(
    val driverId: String,
    val metricDate: LocalDate,
    val totalStops: Int,
    val ontimeStops: Int,
    val lateStops: Int,
    val otdPercentage: Double,
    val avgDelayMinutes: Int,
    val maxDelayMinutes: Int
)

data class DriverPerformanceSummaryDto(
    val driverId: String,
    val driverName: String,
    val period: String,
    val totalStops: Int,
    val ontimeStops: Int,
    val lateStops: Int,
    val otdPercentage: Double,
    val avgDelayMinutes: Int,
    val maxDelayMinutes: Int,
    val dailyBreakdown: List<DriverPerformanceDto>
)