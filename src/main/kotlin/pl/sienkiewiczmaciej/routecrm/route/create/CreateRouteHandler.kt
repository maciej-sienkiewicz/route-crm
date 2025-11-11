// route/create/CreateRouteHandler.kt (UPDATED WITH EVENTS)
package pl.sienkiewiczmaciej.routecrm.route.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteCreatedEvent
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteScheduleAddedEvent
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.LocalDate
import java.time.LocalTime

data class RouteStopData(
    val stopOrder: Int,
    val stopType: StopType,
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress
)

data class CreateRouteCommand(
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val stops: List<RouteStopData>
)

data class CreateRouteResult(
    val id: RouteId,
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: DriverId,
    val vehicleId: VehicleId,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val stopsCount: Int
)

@Component
class CreateRouteHandler(
    private val validatorComposite: CreateRouteValidatorComposite,
    private val routeFactory: RouteFactory,
    private val stopFactory: RouteStopFactory,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateRouteCommand): CreateRouteResult {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Validate (throws exception on failure, returns context with pre-loaded data)
        val context = validatorComposite.validate(command)

        // 3. Create route domain object
        val route = routeFactory.create(command, context)

        // 4. Persist route first (to get ID for stops)
        val savedRoute = routeRepository.save(route)

        // 5. Create and persist stops
        val stops = stopFactory.createStops(
            routeId = savedRoute.id,
            companyId = command.companyId,
            stopsData = command.stops,
            context = context
        )
        stopRepository.saveAll(stops)

        // 6. Publish domain event
        eventPublisher.publish(
            RouteCreatedEvent(
                aggregateId = savedRoute.id.value,
                routeId = savedRoute.id,
                companyId = savedRoute.companyId,
                routeName = savedRoute.routeName,
                date = savedRoute.date,
                driverId = savedRoute.driverId,
                vehicleId = savedRoute.vehicleId,
                createdBy = principal.userId,
                createdByName = "${principal.firstName} ${principal.lastName}"
            )
        )

        val stopsBySchedule: Map<ScheduleId, List<RouteStop>> = stops.groupBy { it.scheduleId }
        context.schedules
            .forEach {
                eventPublisher.publish(RouteScheduleAddedEvent(
                    aggregateId = savedRoute.id.value,
                    companyId = principal.companyId,
                    routeId = savedRoute.id,
                    scheduleId = it.key,
                    childId = it.value.childId,
                    pickupStop = stopsBySchedule[it.key]!!.first { it.stopType == StopType.PICKUP },
                    dropoffStop = stopsBySchedule[it.key]!!.first { it.stopType == StopType.DROPOFF },
                    addedBy = principal.userId,
                    routeDate = route.date
                ))
            }

        // 7. Return result
        return CreateRouteResult(
            id = savedRoute.id,
            companyId = savedRoute.companyId,
            routeName = savedRoute.routeName,
            date = savedRoute.date,
            status = savedRoute.status,
            driverId = savedRoute.driverId,
            vehicleId = savedRoute.vehicleId,
            estimatedStartTime = savedRoute.estimatedStartTime,
            estimatedEndTime = savedRoute.estimatedEndTime,
            stopsCount = stops.size
        )
    }
}