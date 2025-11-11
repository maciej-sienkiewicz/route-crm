// route/domain/events/RouteEvents.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.events

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEvent
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class RouteCreatedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "Route",
    val routeId: RouteId,
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val createdBy: UserId,
    val createdByName: String
) : DomainEvent

data class RouteScheduleAddedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "Route",
    val companyId: CompanyId,
    val routeId: RouteId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val pickupStop: RouteStop,
    val dropoffStop: RouteStop,
    val addedBy: UserId,
    val routeDate: LocalDate,
) : DomainEvent

data class RouteStopExecutedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "RouteStop",
    val routeId: RouteId,
    val companyId: CompanyId,
    val stopId: RouteStopId,
    val stopType: StopType,
    val childId: ChildId,
    val executionStatus: ExecutionStatus,
    val actualTime: Instant,
    val executedBy: UserId
) : DomainEvent

data class RouteStatusChangedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "Route",
    val companyId: CompanyId,
    val routeId: RouteId,
    val previousStatus: RouteStatus,
    val newStatus: RouteStatus,
    val changedBy: UserId,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?
) : DomainEvent

data class RouteScheduleCancelledEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "Route",
    val companyId: CompanyId,
    val routeId: RouteId,
    val scheduleId: ScheduleId,
    val pickupStopId: RouteStopId,
    val dropoffStopId: RouteStopId,
    val cancelledBy: UserId,
    val reason: String
) : DomainEvent

data class RouteScheduleDeletedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "Route",
    val companyId: CompanyId,
    val routeId: RouteId,
    val scheduleId: ScheduleId,
    val deletedBy: UserId
) : DomainEvent

data class RouteStopsReorderedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "Route",
    val companyId: CompanyId,
    val routeId: RouteId,
    val reorderedBy: UserId,
    val stopsCount: Int
) : DomainEvent

data class RouteStopUpdatedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "RouteStop",
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopId: RouteStopId,
    val updatedBy: UserId
) : DomainEvent