// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/listeners/RouteActivityListener.kt
package pl.sienkiewiczmaciej.routecrm.activity.listeners

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.activity.domain.*
import pl.sienkiewiczmaciej.routecrm.route.domain.events.*

@Component
class RouteActivityListener(
    private val activityLogRepository: ActivityLogRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Async
    fun onRouteCreated(event: RouteCreatedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_CREATED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = "Utworzono trasę",
                description = "Utworzono nową trasę \"${event.routeName}\" na dzień ${event.date}",
                performedBy = ActivityPerformer(
                    userId = event.createdBy,
                    userName = event.createdByName,
                    userRole = null
                ),
                metadata = ActivityMetadata.of(
                    "routeName" to event.routeName,
                    "date" to event.date.toString(),
                    "driverId" to event.driverId.value,
                    "vehicleId" to event.vehicleId.value
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteCreated event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteCreated event", e)
        }
    }

    @EventListener
    @Async
    fun onRouteStatusChanged(event: RouteStatusChangedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_STATUS_CHANGED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = "Zmieniono status trasy",
                description = "Status zmieniony z ${event.previousStatus} na ${event.newStatus}",
                performedBy = ActivityPerformer.system(), // TODO: get from event
                metadata = ActivityMetadata.of(
                    "previousStatus" to event.previousStatus.name,
                    "newStatus" to event.newStatus.name,
                    "actualStartTime" to event.actualStartTime?.toString(),
                    "actualEndTime" to event.actualEndTime?.toString()
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteStatusChanged event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteStatusChanged event", e)
        }
    }

    @EventListener
    @Async
    fun onRouteScheduleAdded(event: RouteScheduleAddedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_SCHEDULE_ADDED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = "Dodano harmonogram do trasy",
                description = "Dodano harmonogram dla dziecka do trasy",
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "scheduleId" to event.scheduleId.value,
                    "childId" to event.childId.value,
                    "pickupStopId" to event.pickupStop.id.value,
                    "dropoffStopId" to event.dropoffStop.id.value
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteScheduleAdded event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteScheduleAdded event", e)
        }
    }

    @EventListener
    @Async
    fun onRouteStopExecuted(event: RouteStopExecutedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_STOP_EXECUTED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = when (event.stopType) {
                    pl.sienkiewiczmaciej.routecrm.route.domain.StopType.PICKUP -> "Wykonano odbiór dziecka"
                    pl.sienkiewiczmaciej.routecrm.route.domain.StopType.DROPOFF -> "Wykonano dowóz dziecka"
                },
                description = "Status: ${event.executionStatus}",
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "stopId" to event.stopId.value,
                    "stopType" to event.stopType.name,
                    "childId" to event.childId.value,
                    "executionStatus" to event.executionStatus.name,
                    "actualTime" to event.actualTime.toString()
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteStopExecuted event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteStopExecuted event", e)
        }
    }

    @EventListener
    @Async
    fun onRouteScheduleCancelled(event: RouteScheduleCancelledEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_SCHEDULE_CANCELLED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = "Anulowano harmonogram w trasie",
                description = "Anulowano harmonogram: ${event.reason}",
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "scheduleId" to event.scheduleId.value,
                    "reason" to event.reason,
                    "pickupStopId" to event.pickupStopId.value,
                    "dropoffStopId" to event.dropoffStopId.value
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteScheduleCancelled event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteScheduleCancelled event", e)
        }
    }

    @EventListener
    @Async
    fun onRouteScheduleDeleted(event: RouteScheduleDeletedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_SCHEDULE_DELETED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = "Usunięto harmonogram z trasy",
                description = "Usunięto harmonogram z trasy",
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "scheduleId" to event.scheduleId.value
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteScheduleDeleted event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteScheduleDeleted event", e)
        }
    }

    @EventListener
    @Async
    fun onRouteStopsReordered(event: RouteStopsReorderedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_STOPS_REORDERED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = "Zmieniono kolejność przystanków",
                description = "Zmieniono kolejność ${event.stopsCount} przystanków w trasie",
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "stopsCount" to event.stopsCount
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteStopsReordered event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteStopsReordered event", e)
        }
    }

    @EventListener
    @Async
    fun onRouteStopUpdated(event: RouteStopUpdatedEvent) = runBlocking {
        try {
            val activity = ActivityLog.create(
                companyId = event.companyId,
                category = ActivityCategory.ROUTE,
                activityType = ActivityType.ROUTE_STOP_UPDATED,
                aggregateId = event.routeId.value,
                aggregateType = "Route",
                title = "Zaktualizowano przystanek",
                description = "Zaktualizowano dane przystanku w trasie",
                performedBy = ActivityPerformer.system(),
                metadata = ActivityMetadata.of(
                    "stopId" to event.stopId.value
                ),
                eventId = event.eventId
            )

            activityLogRepository.save(activity)
            logger.debug("Activity log created for RouteStopUpdated event: ${event.routeId.value}")
        } catch (e: Exception) {
            logger.error("Failed to create activity log for RouteStopUpdated event", e)
        }
    }
}