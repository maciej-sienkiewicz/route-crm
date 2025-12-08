package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyPassengerMetrics
import java.time.LocalDate

@Entity
@Table(
    name = "daily_passenger_metrics",
    indexes = [
        Index(name = "idx_passenger_metrics_company_date", columnList = "company_id, metric_date")
    ]
)
class DailyPassengerMetricsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate,

    @Column(name = "avg_trip_duration_minutes", nullable = false)
    val avgTripDurationMinutes: Double,

    @Column(name = "p50_trip_duration_minutes", nullable = false)
    val p50TripDurationMinutes: Int,

    @Column(name = "p90_trip_duration_minutes", nullable = false)
    val p90TripDurationMinutes: Int,

    @Column(name = "p99_trip_duration_minutes", nullable = false)
    val p99TripDurationMinutes: Int,

    @Column(name = "total_trips", nullable = false)
    val totalTrips: Int,

    @Column(name = "trips_over_p90_count", nullable = false)
    val tripsOverP90Count: Int
) {
    fun toDomain() = DailyPassengerMetrics(
        companyId = CompanyId(companyId),
        metricDate = metricDate,
        avgTripDurationMinutes = avgTripDurationMinutes,
        p50TripDurationMinutes = p50TripDurationMinutes,
        p90TripDurationMinutes = p90TripDurationMinutes,
        p99TripDurationMinutes = p99TripDurationMinutes,
        totalTrips = totalTrips,
        tripsOverP90Count = tripsOverP90Count
    )

    companion object {
        fun fromDomain(metrics: DailyPassengerMetrics) = DailyPassengerMetricsEntity(
            companyId = metrics.companyId.value,
            metricDate = metrics.metricDate,
            avgTripDurationMinutes = metrics.avgTripDurationMinutes,
            p50TripDurationMinutes = metrics.p50TripDurationMinutes,
            p90TripDurationMinutes = metrics.p90TripDurationMinutes,
            p99TripDurationMinutes = metrics.p99TripDurationMinutes,
            totalTrips = metrics.totalTrips,
            tripsOverP90Count = metrics.tripsOverP90Count
        )
    }
}