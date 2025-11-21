// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/addchild/AddChildToRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.ConflictResolution
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.SeriesConflictResolver
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class AddChildToRouteSeriesCommand(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val pickupStopOrder: Int,
    val dropoffStopOrder: Int,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null
)

data class AddChildToSeriesResult(
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val existingRoutesUpdated: Int,
    val conflictResolved: Boolean = false
)

@Component
class AddChildToRouteSeriesHandler(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val scheduleRepository: ScheduleRepository,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val conflictResolver: SeriesConflictResolver,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: AddChildToRouteSeriesCommand
    ): AddChildToSeriesResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val series = routeSeriesRepository.findById(command.companyId, command.seriesId)
            ?: throw RouteSeriesNotFoundException(command.seriesId)

        require(series.status == RouteSeriesStatus.ACTIVE) {
            "Cannot add child to series with status ${series.status}"
        }

        val schedule = scheduleRepository.findById(command.companyId, command.scheduleId)
            ?: throw IllegalArgumentException("Schedule ${command.scheduleId.value} not found")

        require(schedule.childId == command.childId) {
            "Schedule does not belong to child ${command.childId.value}"
        }

        val conflictResolution = conflictResolver.resolveAddChildConflict(
            companyId = command.companyId,
            seriesId = command.seriesId,
            scheduleId = command.scheduleId,
            requestedFrom = command.effectiveFrom
        )

        val (effectiveFrom, effectiveTo, conflictResolved) = when (conflictResolution) {
            is ConflictResolution.NoConflict -> {
                Triple(
                    conflictResolution.effectiveFrom,
                    command.effectiveTo,
                    false
                )
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

        val affectedRoutes = routeRepository.findBySeries(
            companyId = command.companyId,
            seriesId = command.seriesId,
            fromDate = effectiveFrom,
            statuses = setOf(RouteStatus.PLANNED)
        )

        var routesUpdated = 0

        for (route in affectedRoutes) {
            if (effectiveTo != null && route.date.isAfter(effectiveTo)) {
                continue
            }

            val existingStops = stopRepository.findByRoute(
                companyId = command.companyId,
                routeId = route.id
            )
            val childAlreadyInRoute = existingStops.any {
                it.childId == command.childId && !it.isCancelled
            }

            if (!childAlreadyInRoute) {
                val pickupStop = RouteStop.create(
                    companyId = command.companyId,
                    routeId = route.id,
                    stopOrder = command.pickupStopOrder,
                    stopType = StopType.PICKUP,
                    childId = command.childId,
                    scheduleId = command.scheduleId,
                    estimatedTime = schedule.pickupTime,
                    address = schedule.pickupAddress
                )

                val dropoffStop = RouteStop.create(
                    companyId = command.companyId,
                    routeId = route.id,
                    stopOrder = command.dropoffStopOrder,
                    stopType = StopType.DROPOFF,
                    childId = command.childId,
                    scheduleId = command.scheduleId,
                    estimatedTime = schedule.dropoffTime,
                    address = schedule.dropoffAddress
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

class RouteSeriesNotFoundException(id: RouteSeriesId) :
    RuntimeException("Route series ${id.value} not found")