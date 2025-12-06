// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/updatestop/UpdateRouteStopValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.updatestop

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException

/**
 * Validation context for UpdateRouteStop operation.
 */
data class UpdateRouteStopValidationContext(
    val route: Route,
    val stop: RouteStop
)

class RouteStopNotFoundException(stopId: pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId) :
    NotFoundException("Route stop ${stopId.value} not found")

/**
 * Composite validator for UpdateRouteStop operation.
 */
@Component
class UpdateRouteStopValidatorComposite(
    private val contextBuilder: UpdateRouteStopContextBuilder,
    private val routeStatusValidator: UpdateStopRouteStatusValidator,
    private val stopBelongsToRouteValidator: UpdateStopBelongsToRouteValidator,
    private val stopModifiableValidator: UpdateStopModifiableValidator
) {
    suspend fun validate(command: UpdateRouteStopCommand): UpdateRouteStopValidationContext {
        // 1. Build context
        val context = contextBuilder.build(command)

        // 2. Run validators
        routeStatusValidator.validate(context)
        stopBelongsToRouteValidator.validate(command, context)
        stopModifiableValidator.validate(context)

        // 3. Return context
        return context
    }
}

@Component
class UpdateStopRouteStatusValidator {
    fun validate(context: UpdateRouteStopValidationContext) {
        require(context.route.status == RouteStatus.PLANNED || context.route.status == RouteStatus.IN_PROGRESS)  {
            "Cannot update stops in route with status ${context.route.status}"
        }
    }
}

@Component
class UpdateStopBelongsToRouteValidator {
    fun validate(command: UpdateRouteStopCommand, context: UpdateRouteStopValidationContext) {
        require(context.stop.routeId == command.routeId) {
            "Stop ${command.stopId.value} does not belong to route ${command.routeId.value}"
        }
    }
}

@Component
class UpdateStopModifiableValidator {
    fun validate(context: UpdateRouteStopValidationContext) {
        require(context.stop.canBeModified()) {
            "Cannot update stop: ${
                when {
                    context.stop.isExecuted() -> "already executed"
                    context.stop.isCancelled -> "cancelled"
                    else -> "not modifiable"
                }
            }"
        }
    }
}