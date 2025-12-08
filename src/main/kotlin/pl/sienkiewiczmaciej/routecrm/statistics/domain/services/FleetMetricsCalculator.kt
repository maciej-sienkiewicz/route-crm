package pl.sienkiewiczmaciej.routecrm.statistics.domain.services

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyFleetMetrics
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleStatus
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.LocalDate

@Service
class FleetMetricsCalculator(
    private val routeJpaRepository: RouteJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val driverJpaRepository: DriverJpaRepository
) {
    suspend fun calculate(companyId: CompanyId, date: LocalDate): DailyFleetMetrics {
        val routes = routeJpaRepository.findByFilters(
            companyId.value,
            date,
            null,
            null,
            org.springframework.data.domain.Pageable.unpaged()
        ).content

        val activeRoutes = routes.filter {
            it.status in setOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS, RouteStatus.COMPLETED)
        }

        val vehiclesInRoutes = activeRoutes.mapNotNull { it.vehicleId }.toSet()
        val driversInRoutes = activeRoutes.mapNotNull { it.driverId }.toSet()
        val routesWithoutDriver = activeRoutes.count { it.driverId == null }

        val totalVehicles = vehicleJpaRepository.findByCompanyIdAndStatus(
            companyId.value,
            VehicleStatus.AVAILABLE,
            org.springframework.data.domain.Pageable.unpaged()
        ).totalElements.toInt() + vehicleJpaRepository.findByCompanyIdAndStatus(
            companyId.value,
            VehicleStatus.IN_ROUTE,
            org.springframework.data.domain.Pageable.unpaged()
        ).totalElements.toInt()

        val totalDrivers = driverJpaRepository.findByCompanyIdAndStatus(
            companyId.value,
            DriverStatus.ACTIVE,
            org.springframework.data.domain.Pageable.unpaged()
        ).totalElements.toInt()

        return DailyFleetMetrics(
            companyId = companyId,
            metricDate = date,
            totalVehicles = totalVehicles,
            vehiclesInRoutes = vehiclesInRoutes.size,
            vehiclesAvailable = totalVehicles - vehiclesInRoutes.size,
            totalDrivers = totalDrivers,
            driversInRoutes = driversInRoutes.size,
            driversAvailable = totalDrivers - driversInRoutes.size,
            routesWithoutDriver = routesWithoutDriver
        )
    }
}