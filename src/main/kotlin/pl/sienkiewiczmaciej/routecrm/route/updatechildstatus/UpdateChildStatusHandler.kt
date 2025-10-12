package pl.sienkiewiczmaciej.routecrm.route.updatechildstatus

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.ChildInRouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteChildRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class UpdateChildStatusCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val childId: ChildId,
    val status: ChildInRouteStatus,
    val actualPickupTime: Instant?,
    val actualDropoffTime: Instant?
)

data class UpdateChildStatusResult(
    val childId: ChildId,
    val status: ChildInRouteStatus,
    val actualPickupTime: Instant?,
    val actualDropoffTime: Instant?
)

class RouteChildNotFoundException(routeId: RouteId, childId: ChildId) :
    NotFoundException("Child ${childId.value} not found in route ${routeId.value}")

@Component
class UpdateChildStatusHandler(
    private val routeRepository: RouteRepository,
    private val routeChildRepository: RouteChildRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateChildStatusCommand): UpdateChildStatusResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        if (principal.role == UserRole.DRIVER && principal.driverId != null) {
            require(route.driverId == DriverId.from(principal.driverId)) {
                "Driver can only update children in their own routes"
            }
        }

        val routeChild = routeChildRepository.findByRouteAndChild(
            command.companyId,
            command.routeId,
            command.childId
        ) ?: throw RouteChildNotFoundException(command.routeId, command.childId)

        val updated = when (command.status) {
            ChildInRouteStatus.IN_VEHICLE -> {
                require(command.actualPickupTime != null) {
                    "Actual pickup time is required when picking up a child"
                }
                routeChild.pickup(command.actualPickupTime)
            }
            ChildInRouteStatus.DELIVERED -> {
                require(command.actualDropoffTime != null) {
                    "Actual dropoff time is required when delivering a child"
                }
                routeChild.deliver(command.actualDropoffTime)
            }
            ChildInRouteStatus.ABSENT -> {
                routeChild.markAbsent()
            }
            ChildInRouteStatus.PENDING -> {
                throw IllegalArgumentException("Cannot change child back to PENDING status")
            }
        }

        val saved = routeChildRepository.save(updated)

        return UpdateChildStatusResult(
            childId = saved.childId,
            status = saved.status,
            actualPickupTime = saved.actualPickupTime,
            actualDropoffTime = saved.actualDropoffTime
        )
    }
}