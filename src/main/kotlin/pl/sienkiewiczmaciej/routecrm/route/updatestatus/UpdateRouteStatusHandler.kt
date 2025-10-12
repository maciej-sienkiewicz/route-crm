package pl.sienkiewiczmaciej.routecrm.route.updatestatus

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class UpdateRouteStatusCommand(
    val companyId: CompanyId,
    val id: RouteId,
    val status: RouteStatus,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?
)

data class UpdateStatusResult(
    val id: RouteId,
    val status: RouteStatus,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?
)

@Component
class UpdateRouteStatusHandler(
    private val routeRepository: RouteRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateRouteStatusCommand): UpdateStatusResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.id)
            ?: throw RouteNotFoundException(command.id)

        if (principal.role == UserRole.DRIVER && principal.driverId != null) {
            require(route.driverId == DriverId.from(principal.driverId)) {
                "Driver can only update their own routes"
            }
        }

        val updated = when (command.status) {
            RouteStatus.IN_PROGRESS -> {
                require(command.actualStartTime != null) {
                    "Actual start time is required when starting a route"
                }
                route.start(command.actualStartTime)
            }
            RouteStatus.COMPLETED -> {
                require(command.actualEndTime != null) {
                    "Actual end time is required when completing a route"
                }
                route.complete(command.actualEndTime)
            }
            RouteStatus.CANCELLED -> {
                route.cancel()
            }
            RouteStatus.PLANNED -> {
                throw IllegalArgumentException("Cannot change route back to PLANNED status")
            }
        }

        val saved = routeRepository.save(updated)

        return UpdateStatusResult(
            id = saved.id,
            status = saved.status,
            actualStartTime = saved.actualStartTime,
            actualEndTime = saved.actualEndTime
        )
    }
}