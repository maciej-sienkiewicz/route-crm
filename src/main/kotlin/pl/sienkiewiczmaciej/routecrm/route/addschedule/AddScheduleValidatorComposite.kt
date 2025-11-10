// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/addschedule/AddRouteScheduleValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.addschedule

import org.springframework.stereotype.Component


/**
 * Composite validator for AddRouteSchedule operation.
 * Coordinates validation and builds context with pre-loaded data.
 */
@Component
class AddScheduleValidatorComposite(
    private val contextBuilder: AddScheduleContextBuilder,
    private val routeStatusValidator: AddScheduleRouteStatusValidator,
    private val childStatusValidator: AddScheduleChildStatusValidator,
    private val scheduleOwnershipValidator: AddScheduleScheduleOwnershipValidator,
    private val childNotInRouteValidator: AddScheduleChildNotInRouteValidator,
    private val stopOrderValidator: AddScheduleStopOrderValidator
) {
    /**
     * Validates the AddRouteSchedule command and returns validation context.
     * Context contains pre-loaded data that the handler can reuse.
     *
     * @throws IllegalArgumentException if any validation fails
     * @throws RouteNotFoundException if route not found
     * @throws ChildNotFoundException if child not found
     * @return AddRouteScheduleValidationContext with pre-loaded aggregates
     */
    suspend fun validate(command: AddRouteScheduleCommand): AddRouteScheduleValidationContext {
        // 1. Build validation context (batch load all data)
        val context = contextBuilder.build(command)

        // 2. Run all validators
        routeStatusValidator.validate(context)
        childStatusValidator.validate(context)
        scheduleOwnershipValidator.validate(command, context)
        childNotInRouteValidator.validate(command, context)
        stopOrderValidator.validate(command)

        // 3. Return context for handler to use
        return context
    }
}

// Individual validators
@Component
class AddScheduleRouteStatusValidator {
    fun validate(context: AddRouteScheduleValidationContext) {
        require(context.route.status == pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus.PLANNED) {
            "Cannot add schedule to route with status ${context.route.status}"
        }
    }
}

@Component
class AddScheduleChildStatusValidator {
    fun validate(context: AddRouteScheduleValidationContext) {
        require(context.child.status == pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus.ACTIVE) {
            "Child ${context.child.id.value} must be ACTIVE to be assigned to a route"
        }
    }
}

@Component
class AddScheduleScheduleOwnershipValidator {
    fun validate(command: AddRouteScheduleCommand, context: AddRouteScheduleValidationContext) {
        require(context.schedule.childId == command.childId) {
            "Schedule ${command.scheduleId.value} does not belong to child ${command.childId.value}"
        }
    }
}

@Component
class AddScheduleChildNotInRouteValidator {
    fun validate(command: AddRouteScheduleCommand, context: AddRouteScheduleValidationContext) {
        val childAlreadyInRoute = context.existingStops.any {
            it.childId == command.childId && !it.isCancelled
        }
        require(!childAlreadyInRoute) {
            "Child ${command.childId.value} is already in this route"
        }
    }
}

@Component
class AddScheduleStopOrderValidator {
    fun validate(command: AddRouteScheduleCommand) {
        require(command.pickupStop.stopOrder < command.dropoffStop.stopOrder) {
            "Pickup stop order (${command.pickupStop.stopOrder}) must be before dropoff stop order (${command.dropoffStop.stopOrder})"
        }
    }
}