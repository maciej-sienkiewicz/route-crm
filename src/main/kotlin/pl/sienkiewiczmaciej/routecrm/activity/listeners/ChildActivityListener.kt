// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/listeners/ChildActivityListener.kt
package pl.sienkiewiczmaciej.routecrm.activity.listeners

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.absence.domain.events.AbsenceCreatedEvent
import pl.sienkiewiczmaciej.routecrm.activity.domain.*
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteDeletedEvent
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteScheduleAddedEvent
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteStopExecutedEvent

@Component
class ChildActivityListener(
    private val activityLogRepository: ActivityLogRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Tworzy aktywność dla dziecka gdy zostaje przypisane do trasy
     */
    @EventListener
    @Async
    fun onChildAssignedToRoute(event: RouteScheduleAddedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.CHILD,
                activityType = ActivityType.CHILD_ASSIGNED_TO_ROUTE,
                aggregateId = event.child.id.value,
                aggregateType = "Child",
                title = "Przypisano do trasy",
                description = "Dziecko zostało przypisane do trasy",
                details = ActivityDetails.of(
                    "Data trasy" to event.routeDate.toString(),
                    "Adres odbioru" to event.pickupStop.address.address.toString(),
                    "Adres docelowy" to event.dropoffStop.address.address.toString(),
                ),
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "routeId" to event.routeId.value,
                    "scheduleId" to event.scheduleId.value,
                    "pickupStopId" to event.pickupStop.id.value,
                    "dropoffStopId" to event.dropoffStop.id.value
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for Child assigned to route: ${event.child.id.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for Child assigned to route", e)
        }
    }

    @EventListener
    @Async
    fun onRouteDeleted(event: RouteDeletedEvent) = runBlocking {
        try {
            event
                .affectedChildren
                .forEach {
                    ActivityLog.create(
                        companyId = event.companyId,
                        category = ActivityCategory.CHILD,
                        activityType = ActivityType.ROUTE_DELETED,
                        aggregateId = it.value,
                        aggregateType = "Child",
                        title = "Usunięto trasę do której było przypisane dziecko",
                        description = "Trasa do której było przypisane dziecko została usunięta.",
                        performedBy = ActivityPerformer.fromPrincipal(event.createdBy.userId, event.createdBy.firstName, event.createdBy.lastName, event.createdBy.role.name),
                        details = ActivityDetails.of(
                            "Nazwa trasy" to event.routeName,
                            "Planowana data trasy" to event.date
                        ),
                        metadata = ActivityMetadata.of(
                            "routeId" to event.routeId.value,
                        ),
                        eventId = event.eventId
                    )
                }
        } catch (e: Exception) {
            logger.error("Failed to create activity log for route deleted", e)
        }
    }

    /**
     * Tworzy aktywność dla dziecka gdy zostaje odebrane/dowiezione
     */
    @EventListener
    @Async
    fun onChildStopExecuted(event: RouteStopExecutedEvent) = runBlocking {
        try {
            val title = when (event.stopType) {
                pl.sienkiewiczmaciej.routecrm.route.domain.StopType.PICKUP -> "Odebrano dziecko"
                pl.sienkiewiczmaciej.routecrm.route.domain.StopType.DROPOFF -> "Dowieziono dziecko"
            }

            val description = when (event.executionStatus) {
                pl.sienkiewiczmaciej.routecrm.route.domain.ExecutionStatus.COMPLETED ->
                    "Dziecko zostało ${if (event.stopType == pl.sienkiewiczmaciej.routecrm.route.domain.StopType.PICKUP) "odebrane" else "dowiezione"} zgodnie z planem"
                pl.sienkiewiczmaciej.routecrm.route.domain.ExecutionStatus.NO_SHOW ->
                    "Dziecko nie było dostępne w miejscu ${if (event.stopType == pl.sienkiewiczmaciej.routecrm.route.domain.StopType.PICKUP) "odbioru" else "dowozu"}"
                pl.sienkiewiczmaciej.routecrm.route.domain.ExecutionStatus.REFUSED ->
                    "Odmówiono ${if (event.stopType == pl.sienkiewiczmaciej.routecrm.route.domain.StopType.PICKUP) "odbioru" else "dowozu"} dziecka"
            }

            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.CHILD,
                activityType = ActivityType.CHILD_ASSIGNED_TO_ROUTE, // używamy istniejącego typu
                aggregateId = event.childId.value,
                aggregateType = "Child",
                title = title,
                description = description,
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "routeId" to event.routeId.value,
                    "stopId" to event.stopId.value,
                    "stopType" to event.stopType.name,
                    "executionStatus" to event.executionStatus.name,
                    "actualTime" to event.actualTime.toString()
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for Child stop executed: ${event.childId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for Child stop executed", e)
        }
    }

    @EventListener
    @Async
    fun onAbsenceCreated(event: AbsenceCreatedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ABSENCE,
                aggregateId = event.aggregateId,
                activityType = ActivityType.ABSENCE_CREATED,
                aggregateType = event.aggregateType,
                title = "Dodano nieobecność",
                description = "Dziecko zostało oznaczone jako nieobecne we wprowadzonym zakresie czasu.",
                details = ActivityDetails.of(
                    "Typ nieobecności" to event.absenceType,
                    "Nieobecność od dnia" to event.startDate.toString(),
                    "Nieobecność do dnia" to event.endDate.toString(),
                    "Liczba tras, która uległa zmianie:" to event.affectedRoutes.size,
                ),
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "routeIds" to event.affectedRoutes
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for absence created: ${event.child.id.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for absence created:", e)
        }
    }

    // TODO: Dodać pozostałe wydarzenia dla dziecka gdy będą dostępne:
    // - ChildCreatedEvent
    // - ChildUpdatedEvent
    // - ChildStatusChangedEvent
    // - ChildDeletedEvent
}