// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/create/RouteFactory.kt
package pl.sienkiewiczmaciej.routecrm.route.create

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.Route

/**
 * Factory responsible for creating Route domain objects.
 * Handles complex Route creation logic using validated data from ValidationContext.
 */
@Component
class RouteFactory {

    /**
     * Creates a new Route aggregate from the command and validated context.
     * Assumes all validation has been performed by CreateRouteValidatorComposite.
     *
     * @param command The create route command containing user input
     * @param context Validated context with pre-loaded driver and vehicle
     * @return Newly created Route in PLANNED status
     */
    fun create(
        command: CreateRouteCommand,
        context: CreateRouteValidationContext
    ): Route {
        return Route.create(
            companyId = command.companyId,
            routeName = command.routeName,
            date = command.date,
            driverId = context.driver?.id,
            vehicleId = context.vehicle.id,
            estimatedStartTime = command.estimatedStartTime,
            estimatedEndTime = command.estimatedEndTime
        )
    }
}