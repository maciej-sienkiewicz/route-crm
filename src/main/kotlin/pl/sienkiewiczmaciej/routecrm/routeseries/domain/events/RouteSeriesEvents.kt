// routeseries/domain/events/RouteSeriesEvents.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.domain.events

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEvent
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class RouteSeriesCreatedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "RouteSeries",
    val seriesId: RouteSeriesId,
    val companyId: CompanyId,
    val seriesName: String,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recurrenceInterval: Int,
    val createdBy: UserId,
    val createdByName: String
) : DomainEvent

data class RouteSeriesChildAddedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "RouteSeries",
    val seriesId: RouteSeriesId,
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val addedBy: UserId
) : DomainEvent

data class RouteSeriesChildRemovedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "RouteSeries",
    val seriesId: RouteSeriesId,
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val effectiveFrom: LocalDate,
    val removedBy: UserId
) : DomainEvent

data class RouteSeriesCancelledEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "RouteSeries",
    val seriesId: RouteSeriesId,
    val companyId: CompanyId,
    val reason: String,
    val cancelledBy: UserId
) : DomainEvent