package pl.sienkiewiczmaciej.routecrm.statistics.api.mappers

import pl.sienkiewiczmaciej.routecrm.statistics.api.dto.*
import pl.sienkiewiczmaciej.routecrm.statistics.domain.*

object StatisticsMappers {
    fun DailyFleetMetrics.toDto() = FleetMetricsDto(
        metricDate = metricDate,
        totalVehicles = totalVehicles,
        vehiclesInRoutes = vehiclesInRoutes,
        vehiclesAvailable = vehiclesAvailable,
        totalDrivers = totalDrivers,
        driversInRoutes = driversInRoutes,
        driversAvailable = driversAvailable,
        routesWithoutDriver = routesWithoutDriver
    )

    fun DailyCapacityMetrics.toDto() = CapacityMetricsDto(
        metricDate = metricDate,
        avgSaturationPercent = avgSaturationPercent,
        minSaturationPercent = minSaturationPercent,
        maxSaturationPercent = maxSaturationPercent,
        totalRoutes = totalRoutes,
        routesLowSaturation = routesLowSaturation
    )

    fun DailyWorkloadMetrics.toDto() = WorkloadMetricsDto(
        metricDate = metricDate,
        avgStopsPerVehicle = avgStopsPerVehicle,
        minStopsPerVehicle = minStopsPerVehicle,
        maxStopsPerVehicle = maxStopsPerVehicle,
        stdDevStops = stdDevStops,
        avgStopsPerDriver = avgStopsPerDriver,
        minStopsPerDriver = minStopsPerDriver,
        maxStopsPerDriver = maxStopsPerDriver
    )

    fun DriverDailyPerformance.toDto() = DriverPerformanceDto(
        driverId = driverId.value,
        metricDate = metricDate,
        totalStops = totalStops,
        ontimeStops = ontimeStops,
        lateStops = lateStops,
        otdPercentage = otdPercentage,
        avgDelayMinutes = avgDelayMinutes,
        maxDelayMinutes = maxDelayMinutes
    )

    fun DailyPassengerMetrics.toDto() = PassengerMetricsDto(
        metricDate = metricDate,
        avgTripDurationMinutes = avgTripDurationMinutes,
        p50TripDurationMinutes = p50TripDurationMinutes,
        p90TripDurationMinutes = p90TripDurationMinutes,
        p99TripDurationMinutes = p99TripDurationMinutes,
        totalTrips = totalTrips,
        tripsOverP90Count = tripsOverP90Count
    )
}