// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/reassigndriver/ReassignDriverForSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.reassigndriver

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteDriverAssignment
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteDriverAssignmentRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.addchild.RouteSeriesNotFoundException
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class ReassignDriverForSeriesCommand(
    val companyId: CompanyId,
    val seriesId: RouteSeriesId,
    val newDriverId: DriverId,
    val effectiveFrom: LocalDate,
    val reason: String? = null
)

data class ReassignDriverForSeriesResult(
    val seriesId: RouteSeriesId,
    val previousDriverId: DriverId,
    val newDriverId: DriverId,
    val futureRoutesUpdated: Int
)

@Component
class ReassignDriverForSeriesHandler(
    private val seriesRepository: RouteSeriesRepository,
    private val routeRepository: RouteRepository,
    private val assignmentRepository: RouteDriverAssignmentRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(
        principal: UserPrincipal,
        command: ReassignDriverForSeriesCommand
    ): ReassignDriverForSeriesResult {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Load series
        val series = seriesRepository.findById(command.companyId, command.seriesId)
            ?: throw RouteSeriesNotFoundException(command.seriesId)

        require(series.status == RouteSeriesStatus.ACTIVE) {
            "Cannot reassign driver for cancelled series"
        }

        // 3. Update series
        val previousDriverId = series.driverId
        val updatedSeries = series.copy(driverId = command.newDriverId)
        seriesRepository.save(updatedSeries)

        // 4. Update future materialized routes
        val futureRoutes = routeRepository.findBySeries(
            companyId = command.companyId,
            seriesId = command.seriesId,
            fromDate = command.effectiveFrom,
            statuses = setOf(RouteStatus.PLANNED, RouteStatus.DRIVER_MISSING)
        )

        futureRoutes.forEach { route ->
            // Create assignment record
            val assignment = RouteDriverAssignment.create(
                companyId = command.companyId,
                routeId = route.id,
                previousDriverId = route.driverId!!,
                newDriverId = command.newDriverId,
                reassignedBy = principal.userId,
                reason = command.reason ?: "Series driver reassignment"
            )
            assignmentRepository.save(assignment)

            // Update route
            val updated = route.reassignDriver(command.newDriverId)
            routeRepository.save(updated)
        }

        // 5. Return result
        return ReassignDriverForSeriesResult(
            seriesId = series.id,
            previousDriverId = previousDriverId,
            newDriverId = command.newDriverId,
            futureRoutesUpdated = futureRoutes.size
        )
    }
}