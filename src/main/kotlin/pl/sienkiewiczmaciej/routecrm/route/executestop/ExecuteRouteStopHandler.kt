package pl.sienkiewiczmaciej.routecrm.route.executestop

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteStopExecutedEvent
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.updatestop.RouteStopNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class ExecuteRouteStopCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopId: RouteStopId,
    val actualTime: Instant,
    val status: ExecutionStatus,
    val notes: String?
)

data class ExecuteRouteStopResult(
    val id: RouteStopId,
    val actualTime: Instant,
    val executionStatus: ExecutionStatus,
    val executedByName: String
)

@Component
class ExecuteRouteStopHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val delayEventRepository: RouteDelayEventRepository,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    suspend fun handle(principal: UserPrincipal, command: ExecuteRouteStopCommand): ExecuteRouteStopResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        if (principal.role == UserRole.DRIVER && principal.driverId != null) {
            require(route.driverId == DriverId.from(principal.driverId)) {
                "Driver can only execute stops in their own routes"
            }
        }

        val stop = stopRepository.findById(command.companyId, command.stopId)
            ?: throw RouteStopNotFoundException(command.stopId)

        require(stop.routeId == command.routeId) {
            "Stop ${command.stopId.value} does not belong to route ${command.routeId.value}"
        }

        val executed = stop.execute(
            actualTime = command.actualTime,
            status = command.status,
            executedByUserId = principal.userId.value,
            executedByName = "${principal.firstName} ${principal.lastName}",
            notes = command.notes
        )

        val saved = stopRepository.save(executed)

        detectAndPersistDelayIfNeeded(route, saved)

        eventPublisher.publish(
            RouteStopExecutedEvent(
                aggregateId = saved.id.value,
                routeId = saved.routeId,
                stopId = saved.id,
                stopType = saved.stopType,
                childId = saved.childId,
                executionStatus = saved.executionStatus!!,
                actualTime = saved.actualTime!!,
                executedBy = principal.userId,
                companyId = principal.companyId
            )
        )

        return ExecuteRouteStopResult(
            id = saved.id,
            actualTime = saved.actualTime!!,
            executionStatus = saved.executionStatus!!,
            executedByName = saved.executedByName!!
        )
    }

    private suspend fun detectAndPersistDelayIfNeeded(route: Route, stop: RouteStop) {
        if (stop.actualTime == null) return

        val estimatedDateTime = stop.estimatedTime
            .atDate(route.date)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val delay = Duration.between(estimatedDateTime, stop.actualTime)

        if (delay.toMinutes() >= DELAY_THRESHOLD_MINUTES) {
            delayEventRepository.save(
                RouteDelayEvent.retrospective(
                    companyId = route.companyId,
                    routeId = route.id,
                    stopId = stop.id,
                    delayMinutes = delay.toMinutes().toInt(),
                    detectedAt = Instant.now()
                )
            )

            logger.info(
                "Retrospective delay detected: Route ${route.id.value}, " +
                        "Stop ${stop.id.value}, Delay: ${delay.toMinutes()} minutes"
            )
        }
    }

    companion object {
        private const val DELAY_THRESHOLD_MINUTES = 3L
    }
}