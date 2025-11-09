// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/updatestop/UpdateRouteStopHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.updatestop

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.getbyid.RouteNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.external.GeocodingService
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class UpdateRouteStopCommand(
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopId: RouteStopId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress
)

data class UpdateRouteStopResult(
    val id: RouteStopId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress
)

class RouteStopNotFoundException(stopId: RouteStopId) :
    NotFoundException("Route stop ${stopId.value} not found")

@Component
class UpdateRouteStopHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val geocodingService: GeocodingService,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateRouteStopCommand): UpdateRouteStopResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val route = routeRepository.findById(command.companyId, command.routeId)
            ?: throw RouteNotFoundException(command.routeId)

        require(route.status == RouteStatus.PLANNED) {
            "Cannot update stops in route with status ${route.status}"
        }

        val stop = stopRepository.findById(command.companyId, command.stopId)
            ?: throw RouteStopNotFoundException(command.stopId)

        require(stop.routeId == command.routeId) {
            "Stop ${command.stopId.value} does not belong to route ${command.routeId.value}"
        }

        require(stop.canBeModified()) {
            "Cannot update stop: ${
                when {
                    stop.isExecuted() -> "already executed"
                    stop.isCancelled -> "cancelled"
                    else -> "not modifiable"
                }
            }"
        }

        // Geokoduj nowy adres
        val geocodingResult = geocodingService.geocodeAddress(command.address.address)

        val addressWithCoordinates = if (geocodingResult != null) {
            command.address.copy(
                latitude = geocodingResult.latitude,
                longitude = geocodingResult.longitude
            )
        } else {
            command.address
        }

        val updated = stop.updateDetails(command.estimatedTime, addressWithCoordinates)
        val saved = stopRepository.save(updated)

        return UpdateRouteStopResult(
            id = saved.id,
            estimatedTime = saved.estimatedTime,
            address = saved.address
        )
    }
}