package pl.sienkiewiczmaciej.routecrm.statistics.api.dto

data class StatisticsTrendsDto(
    val metric: String,
    val startDate: String,
    val endDate: String,
    val fleetMetrics: List<FleetMetricsDto>? = null,
    val capacityMetrics: List<CapacityMetricsDto>? = null,
    val workloadMetrics: List<WorkloadMetricsDto>? = null,
    val passengerMetrics: List<PassengerMetricsDto>? = null
)