// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/create/CreateRouteSeriesFromRouteHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.*
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.services.RouteSeriesMaterializationService
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class RouteSeriesScheduleData(
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val pickupStopOrder: Int,
    val dropoffStopOrder: Int
)

data class CreateRouteSeriesFromRouteCommand(
    val companyId: CompanyId,
    val sourceRouteId: RouteId,
    val seriesName: String,
    val recurrenceInterval: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?
)

data class CreateSeriesFromRouteResult(
    val seriesId: RouteSeriesId,
    val seriesName: String,
    val schedulesCount: Int,
    val routesMaterialized: Int
)

@Component
class CreateRouteSeriesFromRouteHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val routeSeriesRepository: RouteSeriesRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val materializationService: RouteSeriesMaterializationService,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: CreateRouteSeriesFromRouteCommand
    ): CreateSeriesFromRouteResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val sourceRoute = routeRepository.findById(
            command.companyId,
            command.sourceRouteId
        ) ?: throw RouteNotFoundException(command.sourceRouteId)

        require(sourceRoute.status == RouteStatus.PLANNED) {
            "Can only create series from PLANNED routes"
        }

        val stops = stopRepository.findByRoute(
            command.companyId,
            command.sourceRouteId
        )

        val scheduleStops = stops.groupBy { it.scheduleId }
            .filter { it.value.size == 2 }
            .map { (scheduleId, stopPair) ->
                val pickup = stopPair.first { it.stopType == StopType.PICKUP }
                val dropoff = stopPair.first { it.stopType == StopType.DROPOFF }

                RouteSeriesScheduleData(
                    scheduleId = scheduleId,
                    childId = pickup.childId,
                    pickupStopOrder = pickup.stopOrder,
                    dropoffStopOrder = dropoff.stopOrder
                )
            }

        require(scheduleStops.isNotEmpty()) {
            "Route has no valid schedule pairs to copy"
        }

        val series = RouteSeries.create(
            companyId = command.companyId,
            seriesName = command.seriesName,
            routeNameTemplate = sourceRoute.routeName,
            driverId = sourceRoute.driverId,
            vehicleId = sourceRoute.vehicleId,
            estimatedStartTime = sourceRoute.estimatedStartTime,
            estimatedEndTime = sourceRoute.estimatedEndTime,
            recurrenceInterval = command.recurrenceInterval,
            startDate = command.startDate,
            endDate = command.endDate
        )

        val savedSeries = routeSeriesRepository.save(series)

        val seriesSchedules = scheduleStops.map { scheduleData ->
            RouteSeriesSchedule.create(
                companyId = command.companyId,
                seriesId = savedSeries.id,
                scheduleId = scheduleData.scheduleId,
                childId = scheduleData.childId,
                pickupStopOrder = scheduleData.pickupStopOrder,
                dropoffStopOrder = scheduleData.dropoffStopOrder,
                validFrom = command.startDate,
                validTo = null,
            )
        }

        seriesScheduleRepository.saveAll(seriesSchedules)

        val materializationResult = materializationService.materializeForDateRange(
            companyId = principal.companyId,
            dateRange = command.startDate..command.startDate.plusDays(14),
            forceRegenerate = false,
        )

        return CreateSeriesFromRouteResult(
            seriesId = savedSeries.id,
            seriesName = savedSeries.seriesName,
            schedulesCount = seriesSchedules.size,
            routesMaterialized = materializationResult.routesCreated
        )
    }
}