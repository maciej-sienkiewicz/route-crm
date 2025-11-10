// route/infrastructure/listeners/RouteAuditListener.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure.listeners

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.events.*

/**
 * Listener for route events that handles audit logging.
 * All methods are async to not block the main transaction.
 */
@Component
class RouteAuditListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Async
    fun onRouteCreated(event: RouteCreatedEvent) {
        logger.info(
            "AUDIT: Route created | RouteId: {} | Name: {} | Date: {} | Driver: {} | Vehicle: {} | By: {}",
            event.routeId.value,
            event.routeName,
            event.date,
            event.driverId.value,
            event.vehicleId.value,
            event.createdByName
        )
    }

    @EventListener
    @Async
    fun onRouteScheduleAdded(event: RouteScheduleAddedEvent) {
        logger.info(
            "AUDIT: Schedule added to route | RouteId: {} | ScheduleId: {} | ChildId: {} | PickupStopId: {} | DropoffStopId: {} | By: {}",
            event.routeId.value,
            event.scheduleId.value,
            event.childId.value,
            event.pickupStopId.value,
            event.dropoffStopId.value,
            event.addedBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteStopExecuted(event: RouteStopExecutedEvent) {
        logger.info(
            "AUDIT: Stop executed | RouteId: {} | StopId: {} | Type: {} | ChildId: {} | Status: {} | ActualTime: {} | By: {}",
            event.routeId.value,
            event.stopId.value,
            event.stopType,
            event.childId.value,
            event.executionStatus,
            event.actualTime,
            event.executedBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteStatusChanged(event: RouteStatusChangedEvent) {
        logger.info(
            "AUDIT: Route status changed | RouteId: {} | From: {} | To: {} | By: {}",
            event.routeId.value,
            event.previousStatus,
            event.newStatus,
            event.changedBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteScheduleCancelled(event: RouteScheduleCancelledEvent) {
        logger.info(
            "AUDIT: Schedule cancelled | RouteId: {} | ScheduleId: {} | Reason: {} | By: {}",
            event.routeId.value,
            event.scheduleId.value,
            event.reason,
            event.cancelledBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteScheduleDeleted(event: RouteScheduleDeletedEvent) {
        logger.info(
            "AUDIT: Schedule deleted | RouteId: {} | ScheduleId: {} | By: {}",
            event.routeId.value,
            event.scheduleId.value,
            event.deletedBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteStopsReordered(event: RouteStopsReorderedEvent) {
        logger.info(
            "AUDIT: Stops reordered | RouteId: {} | StopsCount: {} | By: {}",
            event.routeId.value,
            event.stopsCount,
            event.reorderedBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteStopUpdated(event: RouteStopUpdatedEvent) {
        logger.info(
            "AUDIT: Stop updated | RouteId: {} | StopId: {} | By: {}",
            event.routeId.value,
            event.stopId.value,
            event.updatedBy.value
        )
    }
}