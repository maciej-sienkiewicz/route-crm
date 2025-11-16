// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/upcoming/GetDriverUpcomingRoutesHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes.upcoming

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.LocalDate
import java.time.LocalTime

data class GetDriverUpcomingRoutesQuery(
    val companyId: CompanyId,
    val driverId: DriverId,
    val pageable: Pageable
)

data class DriverUpcomingRouteItem(
    val id: RouteId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val vehicleId: VehicleId,
    val vehicleRegistrationNumber: String,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val stopsCount: Int,
    val childrenCount: Int,
    val firstStopAddress: String,
    val lastStopAddress: String
)

@Component
class GetDriverUpcomingRoutesHandler(
    private val validatorComposite: GetDriverUpcomingRoutesValidatorComposite,
    private val enrichmentService: DriverUpcomingRoutesEnrichmentService,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDriverUpcomingRoutesQuery): Page<DriverUpcomingRouteItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER)
        authService.requireSameCompany(principal.companyId, query.companyId)

        if (principal.role == UserRole.DRIVER) {
            require(principal.driverId != null) { "Driver ID is required" }
            require(principal.driverId == query.driverId.value) {
                "Driver can only access their own upcoming routes"
            }
        }

        val context = validatorComposite.validate(query)

        val items = enrichmentService.enrichRoutes(context.routes, query.companyId)

        return PageImpl(items, context.routes.pageable, context.routes.totalElements)
    }
}