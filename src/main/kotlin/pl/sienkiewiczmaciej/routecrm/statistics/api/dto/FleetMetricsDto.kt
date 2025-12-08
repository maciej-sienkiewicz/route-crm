package pl.sienkiewiczmaciej.routecrm.statistics.api.dto

import java.time.LocalDate

data class FleetMetricsDto(
    val metricDate: LocalDate,
    val totalVehicles: Int,
    val vehiclesInRoutes: Int,
    val vehiclesAvailable: Int,
    val totalDrivers: Int,
    val driversInRoutes: Int,
    val driversAvailable: Int,
    val routesWithoutDriver: Int
)