package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(
    name = "routes",
    indexes = [
        Index(name = "idx_routes_company", columnList = "company_id"),
        Index(name = "idx_routes_company_date", columnList = "company_id, date"),
        Index(name = "idx_routes_company_status", columnList = "company_id, status"),
        Index(name = "idx_routes_company_driver", columnList = "company_id, driver_id"),
        Index(name = "idx_routes_date_status", columnList = "date, status")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: RouteStatus,

    @Column(name = "driver_id", nullable = false, length = 50)
    val driverId: String,

    @Column(name = "vehicle_id", nullable = false, length = 50)
    val vehicleId: String,

    @Column(name = "estimated_start_time", nullable = false)
    val estimatedStartTime: LocalTime,

    @Column(name = "estimated_end_time", nullable = false)
    val estimatedEndTime: LocalTime,

    @Column(name = "actual_start_time")
    val actualStartTime: Instant?,

    @Column(name = "actual_end_time")
    val actualEndTime: Instant?,

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
        status = status,
        driverId = DriverId(driverId),
        vehicleId = VehicleId(vehicleId),
        estimatedStartTime = estimatedStartTime,
        estimatedEndTime = estimatedEndTime,
        actualStartTime = actualStartTime,
        actualEndTime = actualEndTime
    )

    companion object {
        fun fromDomain(route: Route) = RouteEntity(
            id = route.id.value,
            companyId = route.companyId.value,
            routeName = route.routeName,
            date = route.date,
            status = route.status,
            driverId = route.driverId.value,
            vehicleId = route.vehicleId.value,
            estimatedStartTime = route.estimatedStartTime,
            estimatedEndTime = route.estimatedEndTime,
            actualStartTime = route.actualStartTime,
            actualEndTime = route.actualEndTime
        )
    }
}

@Entity
@Table(
    name = "route_children",
    indexes = [
        Index(name = "idx_route_children_company", columnList = "company_id"),
        Index(name = "idx_route_children_route", columnList = "route_id"),
        Index(name = "idx_route_children_child", columnList = "company_id, child_id"),
        Index(name = "idx_route_children_status", columnList = "route_id, status")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_route_child",
            columnNames = ["route_id", "child_id"]
        )
    ]
)
class RouteChildEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "route_id", nullable = false, length = 50)
    val routeId: String,

    @Column(name = "child_id", nullable = false, length = 50)
    val childId: String,

    @Column(name = "schedule_id", nullable = false, length = 50)
    val scheduleId: String,

    @Column(name = "pickup_order", nullable = false)
    val pickupOrder: Int,

    @Column(name = "pickup_address_label", length = 100)
    val pickupAddressLabel: String?,

    @Column(name = "pickup_address_street", nullable = false, length = 255)
    val pickupAddressStreet: String,

    @Column(name = "pickup_address_house_number", nullable = false, length = 20)
    val pickupAddressHouseNumber: String,

    @Column(name = "pickup_address_apartment_number", length = 20)
    val pickupAddressApartmentNumber: String?,

    @Column(name = "pickup_address_postal_code", nullable = false, length = 10)
    val pickupAddressPostalCode: String,

    @Column(name = "pickup_address_city", nullable = false, length = 100)
    val pickupAddressCity: String,

    @Column(name = "pickup_latitude")
    val pickupLatitude: Double?,

    @Column(name = "pickup_longitude")
    val pickupLongitude: Double?,

    @Column(name = "dropoff_address_label", length = 100)
    val dropoffAddressLabel: String?,

    @Column(name = "dropoff_address_street", nullable = false, length = 255)
    val dropoffAddressStreet: String,

    @Column(name = "dropoff_address_house_number", nullable = false, length = 20)
    val dropoffAddressHouseNumber: String,

    @Column(name = "dropoff_address_apartment_number", length = 20)
    val dropoffAddressApartmentNumber: String?,

    @Column(name = "dropoff_address_postal_code", nullable = false, length = 10)
    val dropoffAddressPostalCode: String,

    @Column(name = "dropoff_address_city", nullable = false, length = 100)
    val dropoffAddressCity: String,

    @Column(name = "dropoff_latitude")
    val dropoffLatitude: Double?,

    @Column(name = "dropoff_longitude")
    val dropoffLongitude: Double?,

    @Column(name = "estimated_pickup_time", nullable = false)
    val estimatedPickupTime: LocalTime,

    @Column(name = "estimated_dropoff_time", nullable = false)
    val estimatedDropoffTime: LocalTime,

    @Column(name = "actual_pickup_time")
    val actualPickupTime: Instant?,

    @Column(name = "actual_dropoff_time")
    val actualDropoffTime: Instant?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: ChildInRouteStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = RouteChild(
        id = RouteChildId(id),
        companyId = CompanyId(companyId),
        routeId = RouteId(routeId),
        childId = ChildId(childId),
        scheduleId = ScheduleId(scheduleId),
        pickupOrder = pickupOrder,
        pickupAddress = ScheduleAddress(
            label = pickupAddressLabel,
            address = Address(
                street = pickupAddressStreet,
                houseNumber = pickupAddressHouseNumber,
                apartmentNumber = pickupAddressApartmentNumber,
                postalCode = pickupAddressPostalCode,
                city = pickupAddressCity
            ),
            latitude = pickupLatitude,
            longitude = pickupLongitude
        ),
        dropoffAddress = ScheduleAddress(
            label = dropoffAddressLabel,
            address = Address(
                street = dropoffAddressStreet,
                houseNumber = dropoffAddressHouseNumber,
                apartmentNumber = dropoffAddressApartmentNumber,
                postalCode = dropoffAddressPostalCode,
                city = dropoffAddressCity
            ),
            latitude = dropoffLatitude,
            longitude = dropoffLongitude
        ),
        estimatedPickupTime = estimatedPickupTime,
        estimatedDropoffTime = estimatedDropoffTime,
        actualPickupTime = actualPickupTime,
        actualDropoffTime = actualDropoffTime,
        status = status
    )

    companion object {
        fun fromDomain(routeChild: RouteChild) = RouteChildEntity(
            id = routeChild.id.value,
            companyId = routeChild.companyId.value,
            routeId = routeChild.routeId.value,
            childId = routeChild.childId.value,
            scheduleId = routeChild.scheduleId.value,
            pickupOrder = routeChild.pickupOrder,
            pickupAddressLabel = routeChild.pickupAddress.label,
            pickupAddressStreet = routeChild.pickupAddress.address.street,
            pickupAddressHouseNumber = routeChild.pickupAddress.address.houseNumber,
            pickupAddressApartmentNumber = routeChild.pickupAddress.address.apartmentNumber,
            pickupAddressPostalCode = routeChild.pickupAddress.address.postalCode,
            pickupAddressCity = routeChild.pickupAddress.address.city,
            pickupLatitude = routeChild.pickupAddress.latitude,
            pickupLongitude = routeChild.pickupAddress.longitude,
            dropoffAddressLabel = routeChild.dropoffAddress.label,
            dropoffAddressStreet = routeChild.dropoffAddress.address.street,
            dropoffAddressHouseNumber = routeChild.dropoffAddress.address.houseNumber,
            dropoffAddressApartmentNumber = routeChild.dropoffAddress.address.apartmentNumber,
            dropoffAddressPostalCode = routeChild.dropoffAddress.address.postalCode,
            dropoffAddressCity = routeChild.dropoffAddress.address.city,
            dropoffLatitude = routeChild.dropoffAddress.latitude,
            dropoffLongitude = routeChild.dropoffAddress.longitude,
            estimatedPickupTime = routeChild.estimatedPickupTime,
            estimatedDropoffTime = routeChild.estimatedDropoffTime,
            actualPickupTime = routeChild.actualPickupTime,
            actualDropoffTime = routeChild.actualDropoffTime,
            status = routeChild.status
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