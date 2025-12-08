package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DriverDailyPerformance
import java.time.LocalDate

@Entity
@Table(
    name = "driver_daily_performance",
    indexes = [
        Index(name = "idx_driver_perf_company_date", columnList = "company_id, metric_date"),
        Index(name = "idx_driver_perf_driver_date", columnList = "company_id, driver_id, metric_date")
    ]
)
class DriverDailyPerformanceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "driver_id", nullable = false, length = 50)
    val driverId: String,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,

    @Column(name = "total_stops", nullable = false)
    val totalStops: Int,

    @Column(name = "ontime_stops", nullable = false)
    val ontimeStops: Int,

    @Column(name = "late_stops", nullable = false)
    val lateStops: Int,

    @Column(name = "otd_percentage", nullable = false)
    val otdPercentage: Double,

    @Column(name = "avg_delay_minutes", nullable = false)
    val avgDelayMinutes: Int,

    @Column(name = "max_delay_minutes", nullable = false)
    val maxDelayMinutes: Int
) {
    fun toDomain() = DriverDailyPerformance(
        companyId = CompanyId(companyId),
        driverId = DriverId(driverId),
        metricDate = metricDate,
        totalStops = totalStops,
        ontimeStops = ontimeStops,
        lateStops = lateStops,
        otdPercentage = otdPercentage,
        avgDelayMinutes = avgDelayMinutes,
        maxDelayMinutes = maxDelayMinutes
    )

    companion object {
        fun fromDomain(performance: DriverDailyPerformance) = DriverDailyPerformanceEntity(
            companyId = performance.companyId.value,
            driverId = performance.driverId.value,
            metricDate = performance.metricDate,
            totalStops = performance.totalStops,
            ontimeStops = performance.ontimeStops,
            lateStops = performance.lateStops,
            otdPercentage = performance.otdPercentage,
            avgDelayMinutes = performance.avgDelayMinutes,
            maxDelayMinutes = performance.maxDelayMinutes
        )
    }
}