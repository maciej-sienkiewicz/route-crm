package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

@Entity
@Table(
    name = "route_delay_events",
    indexes = [
        Index(name = "idx_delay_events_route", columnList = "company_id, route_id, detected_at"),
        Index(name = "idx_delay_events_stop", columnList = "company_id, stop_id, detection_type"),
        Index(name = "idx_delay_events_date_range", columnList = "company_id, detected_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_predictive_delay_per_stop",
            columnNames = ["route_id", "stop_id", "detection_type"]
        )
    ]
)
class RouteDelayEventEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "route_id", nullable = false, length = 50)
    val routeId: String,

    @Column(name = "stop_id", nullable = false, length = 50)
    val stopId: String,

    @Column(name = "delay_minutes", nullable = false)
    val delayMinutes: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_type", nullable = false, length = 20)
    val detectionType: DelayDetectionType,

    @Column(name = "detected_at", nullable = false)
    val detectedAt: Instant = Instant.now()
) {
    fun toDomain() = RouteDelayEvent(
        id = RouteDelayEventId(id),
        companyId = CompanyId(companyId),
        routeId = RouteId(routeId),
        stopId = RouteStopId(stopId),
        delayMinutes = delayMinutes,
        detectionType = detectionType,
        detectedAt = detectedAt
    )

    companion object {
        fun fromDomain(event: RouteDelayEvent) = RouteDelayEventEntity(
            id = event.id.value,
            companyId = event.companyId.value,
            routeId = event.routeId.value,
            stopId = event.stopId.value,
            delayMinutes = event.delayMinutes,
            detectionType = event.detectionType,
            detectedAt = event.detectedAt
        )
    }
}