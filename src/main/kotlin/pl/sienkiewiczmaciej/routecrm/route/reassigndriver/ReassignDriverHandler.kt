// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/reassigndriver/ReassignDriverHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.reassigndriver

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.domain.services.DriverAvailabilityChecker
import pl.sienkiewiczmaciej.routecrm.route.domain.services.DriverAvailabilityResult
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class ReassignDriverCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val newDriverId: DriverId,
    val reason: String? = null
)

data class ReassignDriverResult(
    val routeId: RouteId,
    val previousDriverId: DriverId?,
    val newDriverId: DriverId,
    val status: RouteStatus,
    val assignmentId: RouteDriverAssignmentId
)

@Component
class ReassignDriverHandler(
    private val routeRepository: RouteRepository,
    private val assignmentRepository: RouteDriverAssignmentRepository,
    private val driverAvailabilityChecker: DriverAvailabilityChecker,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: ReassignDriverCommand
    ): ReassignDriverResult {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Load route
        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        // 3. Validate new driver availability
        val availability = driverAvailabilityChecker.checkDriverAvailability(
            companyId = command.companyId,
            driverId = command.newDriverId,
            date = route.date
        )

        when (availability) {
            is DriverAvailabilityResult.Available -> {
                // OK - continue
            }
            is DriverAvailabilityResult.Unavailable -> {
                throw IllegalArgumentException(
                    "New driver is not available on ${route.date}: ${availability.reason}"
                )
            }
        }

        // 4. Create assignment record (audit)
        val previousDriverId = route.driverId
        val assignment = RouteDriverAssignment.create(
            companyId = command.companyId,
            routeId = command.routeId,
            previousDriverId = previousDriverId,
            newDriverId = command.newDriverId,
            reassignedBy = principal.userId,
            reason = command.reason
        )
        val savedAssignment = assignmentRepository.save(assignment)

        // 5. Update route (domain logic)
        val updatedRoute = route.reassignDriver(command.newDriverId)
        routeRepository.save(updatedRoute)

        // 6. Return result
        return ReassignDriverResult(
            routeId = updatedRoute.id,
            previousDriverId = previousDriverId,
            newDriverId = command.newDriverId,
            status = updatedRoute.status,
            assignmentId = savedAssignment.id
        )
    }
}