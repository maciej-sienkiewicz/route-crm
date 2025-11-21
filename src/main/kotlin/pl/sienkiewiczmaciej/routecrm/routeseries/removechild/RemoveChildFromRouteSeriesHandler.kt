// routeseries/removechild/RemoveChildFromRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.removechild

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.addchild.RouteSeriesNotFoundException
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.events.RouteSeriesChildRemovedEvent
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

@Component
class RemoveChildFromRouteSeriesHandler(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val childRepository: ChildRepository,
    private val effectiveDateValidator: RemoveChildEffectiveDateValidator,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: RemoveChildFromRouteSeriesCommand
    ): RemoveChildFromSeriesResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val series = routeSeriesRepository.findById(command.companyId, command.seriesId)
            ?: throw RouteSeriesNotFoundException(command.seriesId)

        require(series.status == RouteSeriesStatus.ACTIVE) {
            "Cannot remove child from series with status ${series.status}"
        }

        val seriesSchedule = seriesScheduleRepository.findBySeriesAndSchedule(
            companyId = command.companyId,
            seriesId = command.seriesId,
            scheduleId = command.scheduleId
        ) ?: throw IllegalArgumentException("Schedule ${command.scheduleId.value} not found in series")

        effectiveDateValidator.validate(command.effectiveFrom, seriesSchedule)

        val effectiveTo = command.effectiveFrom.minusDays(1)
        val updated = seriesSchedule.endValidity(effectiveTo)

        seriesScheduleRepository.save(updated)

        var stopsCancelled = 0

        if (command.cancelExistingStops) {
            val affectedRoutes = routeRepository.findBySeries(
                companyId = command.companyId,
                seriesId = command.seriesId,
                fromDate = command.effectiveFrom,
                statuses = setOf(RouteStatus.PLANNED)
            )

            for (route in affectedRoutes) {
                val stops = stopRepository.findByRoute(
                    companyId = command.companyId,
                    routeId = route.id,
                    includeCancelled = false
                ).filter { it.scheduleId == command.scheduleId }

                stops.forEach { stop ->
                    val cancelled = stop.cancel(
                        "Child removed from series effective ${command.effectiveFrom}"
                    )
                    stopRepository.save(cancelled)
                    stopsCancelled++
                }
            }
        }

        val child = childRepository.findById(command.companyId, seriesSchedule.childId)
            ?: throw ChildNotFoundException(seriesSchedule.childId)

        eventPublisher.publish(
            RouteSeriesChildRemovedEvent(
                aggregateId = command.seriesId.value,
                seriesId = command.seriesId,
                companyId = command.companyId,
                scheduleId = command.scheduleId,
                childId = child.id,
                effectiveFrom = command.effectiveFrom,
                removedBy = principal.userId
            )
        )

        return RemoveChildFromSeriesResult(
            seriesId = command.seriesId,
            scheduleId = command.scheduleId,
            effectiveFrom = command.effectiveFrom,
            effectiveTo = effectiveTo,
            stopsCancelled = stopsCancelled
        )
    }
}