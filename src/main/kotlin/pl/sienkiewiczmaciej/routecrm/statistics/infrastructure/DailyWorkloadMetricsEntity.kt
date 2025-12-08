package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyWorkloadMetrics
import java.time.LocalDate

@Entity
@Table(
    name = "daily_workload_metrics",
    indexes = [
        Index(name = "idx_workload_metrics_company_date", columnList = "company_id, metric_date")
    ]
)
class DailyWorkloadMetricsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,

    @Column(name = "avg_stops_per_vehicle", nullable = false)
    val avgStopsPerVehicle: Double,

    @Column(name = "min_stops_per_vehicle", nullable = false)
    val minStopsPerVehicle: Int,

    @Column(name = "max_stops_per_vehicle", nullable = false)
    val maxStopsPerVehicle: Int,

    @Column(name = "std_dev_stops", nullable = false)
    val stdDevStops: Double,

    @Column(name = "avg_stops_per_driver", nullable = false)
    val avgStopsPerDriver: Double,

    @Column(name = "min_stops_per_driver", nullable = false)
    val minStopsPerDriver: Int,

    @Column(name = "max_stops_per_driver", nullable = false)
    val maxStopsPerDriver: Int
) {
    fun toDomain() = DailyWorkloadMetrics(
        companyId = CompanyId(companyId),
        metricDate = metricDate,
        avgStopsPerVehicle = avgStopsPerVehicle,
        minStopsPerVehicle = minStopsPerVehicle,
        maxStopsPerVehicle = maxStopsPerVehicle,
        stdDevStops = stdDevStops,
        avgStopsPerDriver = avgStopsPerDriver,
        minStopsPerDriver = minStopsPerDriver,
        maxStopsPerDriver = maxStopsPerDriver
    )

    companion object {
        fun fromDomain(metrics: DailyWorkloadMetrics) = DailyWorkloadMetricsEntity(
            companyId = metrics.companyId.value,
            metricDate = metrics.metricDate,
            avgStopsPerVehicle = metrics.avgStopsPerVehicle,
            minStopsPerVehicle = metrics.minStopsPerVehicle,
            maxStopsPerVehicle = metrics.maxStopsPerVehicle,
            stdDevStops = metrics.stdDevStops,
            avgStopsPerDriver = metrics.avgStopsPerDriver,
            minStopsPerDriver = metrics.minStopsPerDriver,
            maxStopsPerDriver = metrics.maxStopsPerDriver
        )
    }
}