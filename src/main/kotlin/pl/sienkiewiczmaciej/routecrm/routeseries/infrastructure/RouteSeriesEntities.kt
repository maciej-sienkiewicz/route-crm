// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/infrastructure/RouteSeriesEntities.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(
    name = "route_series",
    indexes = [
        Index(name = "idx_route_series_company", columnList = "company_id"),
        Index(name = "idx_route_series_status", columnList = "company_id, status"),
        Index(name = "idx_route_series_active", columnList = "company_id, status, start_date, end_date")
    ]
)
class RouteSeriesEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "series_name", nullable = false, length = 255)
    val seriesName: String,

    @Column(name = "route_name_template", nullable = false, length = 255)
    val routeNameTemplate: String,

    @Column(name = "driver_id", nullable = false, length = 50)
    val driverId: String,

    @Column(name = "vehicle_id", nullable = false, length = 50)
    val vehicleId: String,

    @Column(name = "estimated_start_time", nullable = false)
    val estimatedStartTime: LocalTime,

    @Column(name = "estimated_end_time", nullable = false)
    val estimatedEndTime: LocalTime,

    @Column(name = "recurrence_interval", nullable = false)
    val recurrenceInterval: Int,  // ← ZMIENIONE: zamiast recurrence_type + recurrence_days

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date")
    val endDate: LocalDate?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: RouteSeriesStatus,

    @Column(name = "cancelled_at")
    val cancelledAt: Instant?,

    @Column(name = "cancelled_by", length = 50)
    val cancelledBy: String?,

    @Column(name = "cancellation_reason", columnDefinition = "text")
    val cancellationReason: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = RouteSeries(
        id = RouteSeriesId(id),
        companyId = CompanyId(companyId),
        seriesName = seriesName,
        routeNameTemplate = routeNameTemplate,
        driverId = DriverId(driverId),
        vehicleId = VehicleId(vehicleId),
        estimatedStartTime = estimatedStartTime,
        estimatedEndTime = estimatedEndTime,
        recurrenceInterval = recurrenceInterval,
        startDate = startDate,
        endDate = endDate,
        status = status,
        cancelledAt = cancelledAt,
        cancelledBy = cancelledBy?.let { UserId(it) },
        cancellationReason = cancellationReason,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(series: RouteSeries) = RouteSeriesEntity(
            id = series.id.value,
            companyId = series.companyId.value,
            seriesName = series.seriesName,
            routeNameTemplate = series.routeNameTemplate,
            driverId = series.driverId.value,
            vehicleId = series.vehicleId.value,
            estimatedStartTime = series.estimatedStartTime,
            estimatedEndTime = series.estimatedEndTime,
            recurrenceInterval = series.recurrenceInterval,
            startDate = series.startDate,
            endDate = series.endDate,
            status = series.status,
            cancelledAt = series.cancelledAt,
            cancelledBy = series.cancelledBy?.value,
            cancellationReason = series.cancellationReason,
            createdAt = series.createdAt
        )
    }
}

@Entity
@Table(
    name = "route_series_schedules",
    indexes = [
        Index(name = "idx_series_schedules_company", columnList = "company_id"),
        Index(name = "idx_series_schedules_series", columnList = "company_id, series_id"),
        Index(name = "idx_series_schedules_validity", columnList = "series_id, schedule_id, valid_from, valid_to")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_series_schedule_validity",
            columnNames = ["series_id", "schedule_id", "valid_from"]
        )
    ]
)
class RouteSeriesScheduleEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "series_id", nullable = false, length = 50)
    val seriesId: String,

    @Column(name = "schedule_id", nullable = false, length = 50)
    val scheduleId: String,

    @Column(name = "child_id", nullable = false, length = 50)
    val childId: String,

    @Column(name = "pickup_stop_order", nullable = false)
    val pickupStopOrder: Int,

    @Column(name = "dropoff_stop_order", nullable = false)
    val dropoffStopOrder: Int,

    @Column(name = "valid_from", nullable = false)
    val validFrom: LocalDate,

    @Column(name = "valid_to")
    val validTo: LocalDate?,

    @Column(name = "added_at", nullable = false)
    val addedAt: Instant = Instant.now()
) {
    fun toDomain() = RouteSeriesSchedule(
        id = RouteSeriesScheduleId(id),
        companyId = pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId(companyId),
        seriesId = RouteSeriesId(seriesId),
        scheduleId = pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId(scheduleId),
        childId = pl.sienkiewiczmaciej.routecrm.child.domain.ChildId(childId),
        pickupStopOrder = pickupStopOrder,
        dropoffStopOrder = dropoffStopOrder,
        validFrom = validFrom,
        validTo = validTo,
        addedAt = addedAt
    )

    companion object {
        fun fromDomain(schedule: RouteSeriesSchedule) = RouteSeriesScheduleEntity(
            id = schedule.id.value,
            companyId = schedule.companyId.value,
            seriesId = schedule.seriesId.value,
            scheduleId = schedule.scheduleId.value,
            childId = schedule.childId.value,
            pickupStopOrder = schedule.pickupStopOrder,
            dropoffStopOrder = schedule.dropoffStopOrder,
            validFrom = schedule.validFrom,
            validTo = schedule.validTo,
            addedAt = schedule.addedAt
        )
    }
}

@Entity
@Table(
    name = "route_series_occurrences",
    indexes = [
        Index(name = "idx_occurrences_company", columnList = "company_id"),
        Index(name = "idx_occurrences_series", columnList = "company_id, series_id"),
        Index(name = "idx_occurrences_date", columnList = "series_id, occurrence_date")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_series_occurrence_date",
            columnNames = ["series_id", "occurrence_date"]
        )
    ]
)
class RouteSeriesOccurrenceEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "series_id", nullable = false, length = 50)
    val seriesId: String,

    @Column(name = "occurrence_date", nullable = false)
    val occurrenceDate: LocalDate,

    @Column(name = "route_id", length = 50)
    val routeId: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: OccurrenceStatus,

    @Column(name = "materialized_at")
    val materializedAt: Instant?
) {
    fun toDomain() = RouteSeriesOccurrence(
        id = RouteSeriesOccurrenceId(id),
        companyId = pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId(companyId),
        seriesId = RouteSeriesId(seriesId),
        occurrenceDate = occurrenceDate,
        routeId = routeId?.let { pl.sienkiewiczmaciej.routecrm.route.domain.RouteId(it) },
        status = status,
        materializedAt = materializedAt
    )

    companion object {
        fun fromDomain(occurrence: RouteSeriesOccurrence) = RouteSeriesOccurrenceEntity(
            id = occurrence.id.value,
            companyId = occurrence.companyId.value,
            seriesId = occurrence.seriesId.value,
            occurrenceDate = occurrence.occurrenceDate,
            routeId = occurrence.routeId?.value,
            status = occurrence.status,
            materializedAt = occurrence.materializedAt
        )
    }
}