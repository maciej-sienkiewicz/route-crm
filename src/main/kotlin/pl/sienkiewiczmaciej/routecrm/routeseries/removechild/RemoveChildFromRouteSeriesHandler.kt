// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/removechild/RemoveChildFromRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.removechild

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.addchild.RouteSeriesNotFoundException
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesScheduleRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.SeriesConflictResolver
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class RemoveChildFromRouteSeriesCommand(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val effectiveFrom: LocalDate,
    val cancelExistingStops: Boolean = true
)

data class RemoveChildFromSeriesResult(
    val seriesId: RouteSeriesId,
    val scheduleId: ScheduleId,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate,
    val stopsCancelled: Int
)

@Component
class RemoveChildFromRouteSeriesHandler(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val conflictResolver: SeriesConflictResolver,
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

        require(series.status == RouteSeriesStatus.ACTIVE || series.status == RouteSeriesStatus.PAUSED) {
            "Cannot remove child from series with status ${series.status}"
        }

        val conflictResolution = conflictResolver.resolveRemoveChildConflict(
            companyId = command.companyId,
            seriesId = command.seriesId,
            scheduleId = command.scheduleId,
            requestedFrom = command.effectiveFrom
        )

        val seriesSchedule = seriesScheduleRepository.findBySeriesAndSchedule(
            companyId = command.companyId,
            seriesId = command.seriesId,
            scheduleId = command.scheduleId
        ) ?: throw IllegalArgumentException("Schedule not found in series")

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
                    routeId = route.id
                ).filter {
                    it.scheduleId == command.scheduleId && !it.isCancelled
                }

                stops.forEach { stop ->
                    val cancelled = stop.cancel(
                        "Child removed from series effective ${command.effectiveFrom}"
                    )
                    stopRepository.save(cancelled)
                    stopsCancelled++
                }
            }
        }

        return RemoveChildFromSeriesResult(
            seriesId = command.seriesId,
            scheduleId = command.scheduleId,
            effectiveFrom = command.effectiveFrom,
            effectiveTo = effectiveTo,
            stopsCancelled = stopsCancelled
        )
    }
}