// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/history/GetRouteHistoryHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.history

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class GetRouteHistoryQuery(
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val pageable: Pageable
)

data class RouteHistoryItem(
    val id: RouteId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: DriverId,
    val driverFirstName: String,
    val driverLastName: String,
    val vehicleId: VehicleId,
    val vehicleRegistrationNumber: String,
    val vehicleModel: String,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val stopsCount: Int,
    val completedStopsCount: Int
)

@Component
class GetRouteHistoryHandler(
    private val validatorComposite: GetRouteHistoryValidatorComposite,
    private val enrichmentService: RouteHistoryEnrichmentService,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetRouteHistoryQuery): Page<RouteHistoryItem> {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        // 2. Validate and build context
        val context = validatorComposite.validate(query)

        // 3. Enrich routes with additional data
        val items = enrichmentService.enrichRoutes(context.routes, query.companyId, query.scheduleId)

        return PageImpl(items, context.routes.pageable, context.routes.totalElements)
    }
}