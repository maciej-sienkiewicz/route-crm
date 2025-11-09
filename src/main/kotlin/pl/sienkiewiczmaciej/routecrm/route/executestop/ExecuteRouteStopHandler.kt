package pl.sienkiewiczmaciej.routecrm.route.executestop

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.updatestop.RouteStopNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

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
    private val authService: AuthorizationService
) {
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

        return ExecuteRouteStopResult(
            id = saved.id,
            actualTime = saved.actualTime!!,
            executionStatus = saved.executionStatus!!,
            executedByName = saved.executedByName!!
        )
    }
}