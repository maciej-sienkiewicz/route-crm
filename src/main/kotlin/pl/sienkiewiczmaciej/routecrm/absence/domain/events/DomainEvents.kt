package pl.sienkiewiczmaciej.routecrm.absence.domain.events

import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEvent
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class AbsenceCreatedEvent(
    override val eventId: String = "EVT-${UUID.randomUUID()}",
    override val occurredAt: Instant = Instant.now(),
    override val aggregateId: String,
    override val aggregateType: String = "Absence",
    val child: Child,
    val companyId: CompanyId,
    val absenceType: AbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
    val createdBy: UserId,
    val createdByName: String,
    val affectedRoutes: List<RouteId>
) : DomainEvent
