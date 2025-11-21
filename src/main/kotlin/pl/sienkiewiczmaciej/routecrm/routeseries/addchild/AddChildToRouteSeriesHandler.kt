// routeseries/addchild/AddChildToRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.events.RouteSeriesChildAddedEvent
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.ConflictResolution
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.SeriesConflictResolver
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

@Component
class AddChildToRouteSeriesHandler(
    private val validatorComposite: AddChildValidatorComposite,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val stopRepository: RouteStopRepository,
    private val conflictResolver: SeriesConflictResolver,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: AddChildToRouteSeriesCommand
    ): AddChildToSeriesResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val context = validatorComposite.validate(command)

        val conflictResolution = conflictResolver.resolveAddChildConflict(
            companyId = command.companyId,
            seriesId = command.seriesId,
            scheduleId = command.scheduleId,
            requestedFrom = command.effectiveFrom
        )

        val (effectiveFrom, effectiveTo, conflictResolved) = when (conflictResolution) {
            is ConflictResolution.NoConflict -> {
                Triple(conflictResolution.effectiveFrom, command.effectiveTo, false)
            }
            is ConflictResolution.Conflict -> {
                Triple(
                    conflictResolution.requestedFrom,
                    command.effectiveTo ?: conflictResolution.limitedTo,
                    true
                )
            }
        }

        val seriesSchedule = RouteSeriesSchedule.create(
            companyId = command.companyId,
            seriesId = command.seriesId,
            scheduleId = command.scheduleId,
            childId = command.childId,
            pickupStopOrder = command.pickupStopOrder,
            dropoffStopOrder = command.dropoffStopOrder,
            validFrom = effectiveFrom,
            validTo = effectiveTo
        )

        seriesScheduleRepository.save(seriesSchedule)

        var routesUpdated = 0

        for (route in context.affectedRoutes) {
            if (effectiveTo != null && route.date.isAfter(effectiveTo)) {
                continue
            }

            val existingStops = stopRepository.findByRoute(
                companyId = command.companyId,
                routeId = route.id,
                includeCancelled = false
            )

            val childAlreadyInRoute = existingStops.any { it.childId == command.childId }
            if (!childAlreadyInRoute) {
                val pickupStop = RouteStop.create(
                    companyId = command.companyId,
                    routeId = route.id,
                    stopOrder = command.pickupStopOrder,
                    stopType = StopType.PICKUP,
                    childId = command.childId,
                    scheduleId = command.scheduleId,
                    estimatedTime = context.schedule.pickupTime,
                    address = context.schedule.pickupAddress
                )

                val dropoffStop = RouteStop.create(
                    companyId = command.companyId,
                    routeId = route.id,
                    stopOrder = command.dropoffStopOrder,
                    stopType = StopType.DROPOFF,
                    childId = command.childId,
                    scheduleId = command.scheduleId,
                    estimatedTime = context.schedule.dropoffTime,
                    address = context.schedule.dropoffAddress
                )

                val reorderedStops = reorderStopsForInsertion(
                    existingStops = existingStops,
                    newPickupOrder = command.pickupStopOrder,
                    newDropoffOrder = command.dropoffStopOrder
                )

                if (reorderedStops.isNotEmpty()) {
                    stopRepository.saveAll(reorderedStops)
                }

                stopRepository.save(pickupStop)
                stopRepository.save(dropoffStop)

                routesUpdated++
            }
        }

        eventPublisher.publish(
            RouteSeriesChildAddedEvent(
                aggregateId = command.seriesId.value,
                seriesId = command.seriesId,
                companyId = command.companyId,
                scheduleId = command.scheduleId,
                childId = command.childId,
                validFrom = effectiveFrom,
                validTo = effectiveTo,
                addedBy = principal.userId
            )
        )

        return AddChildToSeriesResult(
            seriesId = command.seriesId,
            scheduleId = command.scheduleId,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo,
            existingRoutesUpdated = routesUpdated,
            conflictResolved = conflictResolved
        )
    }

    private fun reorderStopsForInsertion(
        existingStops: List<RouteStop>,
        newPickupOrder: Int,
        newDropoffOrder: Int
    ): List<RouteStop> {
        return existingStops.mapNotNull { stop ->
            when {
                stop.stopOrder >= newDropoffOrder ->
                    stop.updateOrder(stop.stopOrder + 2)
                stop.stopOrder >= newPickupOrder && stop.stopOrder < newDropoffOrder ->
                    stop.updateOrder(stop.stopOrder + 1)
                else -> null
            }
        }
    }
}