package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyCapacityMetrics
import java.time.LocalDate

@Entity
@Table(
    name = "daily_capacity_metrics",
    indexes = [
        Index(name = "idx_capacity_metrics_company_date", columnList = "company_id, metric_date")
    ]
)
class DailyCapacityMetricsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,

    @Column(name = "avg_saturation_percent", nullable = false)
    val avgSaturationPercent: Double,

    @Column(name = "min_saturation_percent", nullable = false)
    val minSaturationPercent: Double,

    @Column(name = "max_saturation_percent", nullable = false)
    val maxSaturationPercent: Double,

    @Column(name = "total_routes", nullable = false)
    val totalRoutes: Int,

    @Column(name = "routes_low_saturation", nullable = false)
    val routesLowSaturation: Int
) {
    fun toDomain() = DailyCapacityMetrics(
        companyId = CompanyId(companyId),
        metricDate = metricDate,
        avgSaturationPercent = avgSaturationPercent,
        minSaturationPercent = minSaturationPercent,
        maxSaturationPercent = maxSaturationPercent,
        totalRoutes = totalRoutes,
        routesLowSaturation = routesLowSaturation
    )

    companion object {
        fun fromDomain(metrics: DailyCapacityMetrics) = DailyCapacityMetricsEntity(
            companyId = metrics.companyId.value,
            metricDate = metrics.metricDate,
            avgSaturationPercent = metrics.avgSaturationPercent,
            minSaturationPercent = metrics.minSaturationPercent,
            maxSaturationPercent = metrics.maxSaturationPercent,
            totalRoutes = metrics.totalRoutes,
            routesLowSaturation = metrics.routesLowSaturation
        )
    }
}