package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyFleetMetrics
import java.time.LocalDate

@Entity
@Table(
    name = "daily_fleet_metrics",
    indexes = [
        Index(name = "idx_fleet_metrics_company_date", columnList = "company_id, metric_date")
    ]
)
class DailyFleetMetricsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,

    @Column(name = "total_vehicles", nullable = false)
    val totalVehicles: Int,

    @Column(name = "vehicles_in_routes", nullable = false)
    val vehiclesInRoutes: Int,

    @Column(name = "vehicles_available", nullable = false)
    val vehiclesAvailable: Int,

    @Column(name = "total_drivers", nullable = false)
    val totalDrivers: Int,

    @Column(name = "drivers_in_routes", nullable = false)
    val driversInRoutes: Int,

    @Column(name = "drivers_available", nullable = false)
    val driversAvailable: Int,

    @Column(name = "routes_without_driver", nullable = false)
    val routesWithoutDriver: Int
) {
    fun toDomain() = DailyFleetMetrics(
        companyId = CompanyId(companyId),
        metricDate = metricDate,
        totalVehicles = totalVehicles,
        vehiclesInRoutes = vehiclesInRoutes,
        vehiclesAvailable = vehiclesAvailable,
        totalDrivers = totalDrivers,
        driversInRoutes = driversInRoutes,
        driversAvailable = driversAvailable,
        routesWithoutDriver = routesWithoutDriver
    )

    companion object {
        fun fromDomain(metrics: DailyFleetMetrics) = DailyFleetMetricsEntity(
            companyId = metrics.companyId.value,
            metricDate = metrics.metricDate,
            totalVehicles = metrics.totalVehicles,
            vehiclesInRoutes = metrics.vehiclesInRoutes,
            vehiclesAvailable = metrics.vehiclesAvailable,
            totalDrivers = metrics.totalDrivers,
            driversInRoutes = metrics.driversInRoutes,
            driversAvailable = metrics.driversAvailable,
            routesWithoutDriver = metrics.routesWithoutDriver
        )
    }
}