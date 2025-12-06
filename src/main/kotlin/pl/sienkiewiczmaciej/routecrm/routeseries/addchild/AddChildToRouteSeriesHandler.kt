// routeseries/addchild/AddChildToRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.domain.services.RouteStopOrderingService
import pl.sienkiewiczmaciej.routecrm.route.domain.services.StopInsertionService
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesSchedule
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesScheduleRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.events.RouteSeriesChildAddedEvent
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.ConflictResolution
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.SeriesConflictResolver
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

@Component
class AddChildToRouteSeriesHandler(
    private val validatorComposite: AddChildValidatorComposite,
    private val scheduleConflictValidator: AddChildScheduleConflictValidator,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val stopRepository: RouteStopRepository,
    private val conflictResolver: SeriesConflictResolver,
    private val orderingService: RouteStopOrderingService,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService,
    private val insertionService: StopInsertionService,
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

        // ===== NOWA WALIDACJA KONFLIKTÓW =====
        scheduleConflictValidator.validateAndThrowIfConflicts(
            companyId = command.companyId,
            series = context.series,
            child = context.child,
            scheduleId = command.scheduleId,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo
        )
        // =====================================

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
                val pickupPosition = command.pickupStopOrder

                val reorderedStops = orderingService.insertStopsAt(
                    existingStops = existingStops,
                    insertPosition = pickupPosition,
                    numberOfStopsToInsert = 2
                )

                if (reorderedStops.isNotEmpty()) {
                    stopRepository.saveAll(reorderedStops)
                }

                val pickupStop = RouteStop.create(
                    companyId = command.companyId,
                    routeId = route.id,
                    stopOrder = command.pickupStopOrder,
                    stopType = StopType.PICKUP,
                    childId = command.childId,
                    scheduleId = command.scheduleId,
                    estimatedTime = context.schedule.pickupTime,
                    address = context.schedule.pickupAddress,
                    createdAt = Instant.now()
                )

                val dropoffStop = RouteStop.create(
                    companyId = command.companyId,
                    routeId = route.id,
                    stopOrder = command.dropoffStopOrder,
                    stopType = StopType.DROPOFF,
                    childId = command.childId,
                    scheduleId = command.scheduleId,
                    estimatedTime = context.schedule.dropoffTime,
                    address = context.schedule.dropoffAddress,
                    createdAt = Instant.now()
                )

                insertionService.insertStops(
                    companyId = command.companyId,
                    routeId = route.id,
                    stopsToInsert = listOf(pickupStop, dropoffStop),
                    afterOrder = null
                )

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
}