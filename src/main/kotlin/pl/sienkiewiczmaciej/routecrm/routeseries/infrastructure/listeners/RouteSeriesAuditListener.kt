// routeseries/infrastructure/listeners/RouteSeriesAuditListener.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.infrastructure.listeners

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.events.RouteSeriesCancelledEvent
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.events.RouteSeriesChildAddedEvent
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.events.RouteSeriesChildRemovedEvent
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.events.RouteSeriesCreatedEvent

@Component
class RouteSeriesAuditListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Async
    fun onRouteSeriesCreated(event: RouteSeriesCreatedEvent) {
        logger.info(
            "AUDIT: Route series created | SeriesId: {} | Name: {} | Company: {} | StartDate: {} | EndDate: {} | Interval: {} weeks | By: {}",
            event.seriesId.value,
            event.seriesName,
            event.companyId.value,
            event.startDate,
            event.endDate,
            event.recurrenceInterval,
            event.createdByName
        )
    }

    @EventListener
    @Async
    fun onRouteSeriesChildAdded(event: RouteSeriesChildAddedEvent) {
        logger.info(
            "AUDIT: Child added to series | SeriesId: {} | ScheduleId: {} | ChildId: {} | ValidFrom: {} | ValidTo: {} | By: {}",
            event.seriesId.value,
            event.scheduleId.value,
            event.childId.value,
            event.validFrom,
            event.validTo,
            event.addedBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteSeriesChildRemoved(event: RouteSeriesChildRemovedEvent) {
        logger.info(
            "AUDIT: Child removed from series | SeriesId: {} | ScheduleId: {} | ChildId: {} | EffectiveFrom: {} | By: {}",
            event.seriesId.value,
            event.scheduleId.value,
            event.childId.value,
            event.effectiveFrom,
            event.removedBy.value
        )
    }

    @EventListener
    @Async
    fun onRouteSeriesCancelled(event: RouteSeriesCancelledEvent) {
        logger.info(
            "AUDIT: Route series cancelled | SeriesId: {} | Reason: {} | By: {}",
            event.seriesId.value,
            event.reason,
            event.cancelledBy.value
        )
    }
}