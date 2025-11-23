// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/upcoming/GetUpcomingRoutesHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.upcoming

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.LocalDate
import java.time.LocalTime

data class GetUpcomingRoutesQuery(
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val pageable: Pageable
)

data class UpcomingRouteItem(
    val id: RouteId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: DriverId?,
    val driverFirstName: String,
    val driverLastName: String,
    val vehicleId: VehicleId,
    val vehicleRegistrationNumber: String,
    val vehicleModel: String,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val stopsCount: Int,
    val childStops: List<ChildStopDetail>
)

data class ChildStopDetail(
    val stopId: RouteStopId,
    val stopOrder: Int,
    val stopType: StopType,
    val childFirstName: String,
    val childLastName: String,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress
)

@Component
class GetUpcomingRoutesHandler(
    private val validatorComposite: GetUpcomingRoutesValidatorComposite,
    private val enrichmentService: UpcomingRoutesEnrichmentService,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetUpcomingRoutesQuery): Page<UpcomingRouteItem> {
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