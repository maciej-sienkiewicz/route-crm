// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteEntities.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteEntities.kt

@Entity
@Table(
    name = "routes",
    indexes = [
        Index(name = "idx_routes_company", columnList = "company_id"),
        Index(name = "idx_routes_date", columnList = "company_id, date"),
        Index(name = "idx_routes_driver", columnList = "company_id, driver_id, date"),
        Index(name = "idx_routes_status", columnList = "company_id, status, date"),
        Index(name = "idx_routes_series", columnList = "company_id, series_id, date")  // ← NOWY INDEX
    ]
)
class RouteEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "route_name", nullable = false, length = 255)
    val routeName: String,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(name = "driver_id", nullable = false, length = 50)
    val driverId: String,

    @Column(name = "vehicle_id", nullable = false, length = 50)
    val vehicleId: String,

    @Column(name = "estimated_start_time", nullable = false)
    val estimatedStartTime: LocalTime,

    @Column(name = "estimated_end_time", nullable = false)
    val estimatedEndTime: LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: RouteStatus,

    @Column(name = "actual_start_time")
    val actualStartTime: Instant?,

    @Column(name = "actual_end_time")
    val actualEndTime: Instant?,

    // ========== NOWE POLA ==========
    @Column(name = "series_id", length = 50)
    val seriesId: String?,  // ← DODANE

    @Column(name = "series_occurrence_date")
    val seriesOccurrenceDate: LocalDate?,  // ← DODANE
    // ================================

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = Route(
        id = RouteId(id),
        companyId = CompanyId(companyId),
        routeName = routeName,
        date = date,
        driverId = DriverId(driverId),
        vehicleId = VehicleId(vehicleId),
        estimatedStartTime = estimatedStartTime,
        estimatedEndTime = estimatedEndTime,
        status = status,
        actualStartTime = actualStartTime,
        actualEndTime = actualEndTime,
        seriesId = seriesId?.let { RouteSeriesId(it) },  // ← DODANE
        seriesOccurrenceDate = seriesOccurrenceDate,      // ← DODANE
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(route: Route) = RouteEntity(
            id = route.id.value,
            companyId = route.companyId.value,
            routeName = route.routeName,
            date = route.date,
            driverId = route.driverId.value,
            vehicleId = route.vehicleId.value,
            estimatedStartTime = route.estimatedStartTime,
            estimatedEndTime = route.estimatedEndTime,
            status = route.status,
            actualStartTime = route.actualStartTime,
            actualEndTime = route.actualEndTime,
            seriesId = route.seriesId?.value,              // ← DODANE
            seriesOccurrenceDate = route.seriesOccurrenceDate,  // ← DODANE
            createdAt = route.createdAt
        )
    }
}

@Entity
@Table(
    name = "route_stops",
    indexes = [
        Index(name = "idx_route_stops_company", columnList = "company_id"),
        Index(name = "idx_route_stops_route", columnList = "company_id, route_id"),
        Index(name = "idx_route_stops_route_order", columnList = "route_id, stop_order"),
        Index(name = "idx_route_stops_child", columnList = "company_id, child_id"),
        Index(name = "idx_route_stops_absence", columnList = "company_id, cancelled_by_absence_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_route_stop_order",
            columnNames = ["route_id", "stop_order"]
        )
    ]
)
data class RouteStopEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "route_id", nullable = false, length = 50)
    val routeId: String,

    @Column(name = "stop_order", nullable = false)
    val stopOrder: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false, length = 20)
    val stopType: StopType,

    @Column(name = "child_id", nullable = false, length = 50)
    val childId: String,

    @Column(name = "schedule_id", nullable = false, length = 50)
    val scheduleId: String,

    @Column(name = "estimated_time", nullable = false)
    val estimatedTime: LocalTime,

    @Column(name = "address_label", length = 100)
    val addressLabel: String?,

    @Column(name = "address_street", nullable = false, length = 255)
    val addressStreet: String,

    @Column(name = "address_house_number", nullable = false, length = 20)
    val addressHouseNumber: String,

    @Column(name = "address_apartment_number", length = 20)
    val addressApartmentNumber: String?,

    @Column(name = "address_postal_code", nullable = false, length = 10)
    val addressPostalCode: String,

    @Column(name = "address_city", nullable = false, length = 100)
    val addressCity: String,

    @Column(name = "latitude")
    val latitude: Double?,

    @Column(name = "longitude")
    val longitude: Double?,

    @Column(name = "is_cancelled", nullable = false)
    val isCancelled: Boolean = false,

    @Column(name = "cancelled_by_absence_id", length = 50)
    val cancelledByAbsenceId: String? = null,

    @Column(name = "cancelled_at")
    val cancelledAt: Instant?,

    @Column(name = "cancellation_reason", columnDefinition = "text")
    val cancellationReason: String?,

    @Column(name = "actual_time")
    val actualTime: Instant?,

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", length = 20)
    val executionStatus: ExecutionStatus?,

    @Column(name = "execution_notes", columnDefinition = "text")
    val executionNotes: String?,

    @Column(name = "executed_by_user_id", length = 50)
    val executedByUserId: String?,

    @Column(name = "executed_by_name", length = 255)
    val executedByName: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", insertable = false, updatable = false)
    private val route: RouteEntity? = null

    val routeDate: LocalDate
        get() = route?.date ?: throw IllegalStateException("Route not loaded")

    fun toDomain() = RouteStop(
        id = RouteStopId(id),
        companyId = CompanyId(companyId),
        routeId = RouteId(routeId),
        stopOrder = stopOrder,
        stopType = stopType,
        childId = ChildId(childId),
        scheduleId = ScheduleId(scheduleId),
        estimatedTime = estimatedTime,
        address = ScheduleAddress(
            label = addressLabel,
            address = Address(
                street = addressStreet,
                houseNumber = addressHouseNumber,
                apartmentNumber = addressApartmentNumber,
                postalCode = addressPostalCode,
                city = addressCity
            ),
            latitude = latitude,
            longitude = longitude
        ),
        isCancelled = isCancelled,
        cancelledAt = cancelledAt,
        cancellationReason = cancellationReason,
        actualTime = actualTime,
        executionStatus = executionStatus,
        executionNotes = executionNotes,
        executedByUserId = executedByUserId,
        executedByName = executedByName
    )

    companion object {
        fun fromDomain(stop: RouteStop) = RouteStopEntity(
            id = stop.id.value,
            companyId = stop.companyId.value,
            routeId = stop.routeId.value,
            stopOrder = stop.stopOrder,
            stopType = stop.stopType,
            childId = stop.childId.value,
            scheduleId = stop.scheduleId.value,
            estimatedTime = stop.estimatedTime,
            addressLabel = stop.address.label,
            addressStreet = stop.address.address.street,
            addressHouseNumber = stop.address.address.houseNumber,
            addressApartmentNumber = stop.address.address.apartmentNumber,
            addressPostalCode = stop.address.address.postalCode,
            addressCity = stop.address.address.city,
            latitude = stop.address.latitude,
            longitude = stop.address.longitude,
            isCancelled = stop.isCancelled,
            cancelledAt = stop.cancelledAt,
            cancellationReason = stop.cancellationReason,
            actualTime = stop.actualTime,
            executionStatus = stop.executionStatus,
            executionNotes = stop.executionNotes,
            executedByUserId = stop.executedByUserId,
            executedByName = stop.executedByName
        )
    }
}

@Entity
@Table(
    name = "route_notes",
    indexes = [
        Index(name = "idx_route_notes_company", columnList = "company_id"),
        Index(name = "idx_route_notes_route", columnList = "route_id"),
        Index(name = "idx_route_notes_created", columnList = "route_id, created_at")
    ]
)
class RouteNoteEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "route_id", nullable = false, length = 50)
    val routeId: String,

    @Column(name = "author_user_id", nullable = false, length = 50)
    val authorUserId: String,

    @Column(name = "author_name", nullable = false, length = 255)
    val authorName: String,

    @Column(columnDefinition = "text", nullable = false)
    val content: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = RouteNote(
        id = RouteNoteId(id),
        companyId = CompanyId(companyId),
        routeId = RouteId(routeId),
        authorUserId = authorUserId,
        authorName = authorName,
        content = content,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(note: RouteNote) = RouteNoteEntity(
            id = note.id.value,
            companyId = note.companyId.value,
            routeId = note.routeId.value,
            authorUserId = note.authorUserId,
            authorName = note.authorName,
            content = note.content,
            createdAt = note.createdAt
        )
    }
}