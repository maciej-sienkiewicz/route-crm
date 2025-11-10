// route/updatestop/UpdateRouteStopHandler.kt (UPDATED WITH EVENTS)
package pl.sienkiewiczmaciej.routecrm.route.updatestop

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.events.RouteStopUpdatedEvent
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.domain.events.DomainEventPublisher
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

@Component
class UpdateRouteStopHandler(
    private val validatorComposite: UpdateRouteStopValidatorComposite,
    private val geocodingService: GeocodingService,
    private val stopRepository: RouteStopRepository,
    private val eventPublisher: DomainEventPublisher,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateRouteStopCommand): UpdateRouteStopResult {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        // 2. Validate (throws exception on failure, returns context)
        val context = validatorComposite.validate(command)

        // 3. Geocode new address if needed
        val addressWithCoordinates = if (command.address.latitude == null || command.address.longitude == null) {
            val geocodingResult = geocodingService.geocodeAddress(command.address.address)
            if (geocodingResult != null) {
                command.address.copy(
                    latitude = geocodingResult.latitude,
                    longitude = geocodingResult.longitude
                )
            } else {
                command.address
            }
        } else {
            command.address
        }

        // 4. Update stop using domain method
        val updated = context.stop.updateDetails(command.estimatedTime, addressWithCoordinates)

        // 5. Persist updated stop
        val saved = stopRepository.save(updated)

        // 6. Publish event
        eventPublisher.publish(
            RouteStopUpdatedEvent(
                aggregateId = saved.id.value,
                routeId = saved.routeId,
                stopId = saved.id,
                updatedBy = principal.userId
            )
        )

        // 7. Return result
        return UpdateRouteStopResult(
            id = saved.id,
            estimatedTime = saved.estimatedTime,
            address = saved.address
        )
    }
}