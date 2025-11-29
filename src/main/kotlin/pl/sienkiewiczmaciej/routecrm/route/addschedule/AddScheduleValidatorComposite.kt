package pl.sienkiewiczmaciej.routecrm.route.addschedule

import org.springframework.stereotype.Component

@Component
class AddScheduleValidatorComposite(
    private val contextBuilder: AddScheduleContextBuilder,
    private val routeStatusValidator: AddScheduleRouteStatusValidator,
    private val childStatusValidator: AddScheduleChildStatusValidator,
    private val scheduleOwnershipValidator: AddScheduleScheduleOwnershipValidator,
    private val childNotInRouteValidator: AddScheduleChildNotInRouteValidator,
    private val stopOrderValidator: AddScheduleStopOrderValidator,
    private val absenceValidator: AddScheduleAbsenceValidator
) {
    suspend fun validate(command: AddRouteScheduleCommand): AddRouteScheduleValidationContext {
        val context = contextBuilder.build(command)

        routeStatusValidator.validate(context)
        childStatusValidator.validate(context)
        scheduleOwnershipValidator.validate(command, context)
        childNotInRouteValidator.validate(command, context)
        stopOrderValidator.validate(command, context)
        absenceValidator.validate(command, context)

        return context
    }
}

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
    fun validate(command: AddRouteScheduleCommand, context: AddRouteScheduleValidationContext) {
        val existingStopsCount = context.existingStops.filterNot { it.isCancelled }.size

        require(command.pickupStop.stopOrder > 0) {
            "Pickup stop order must be positive"
        }

        require(command.dropoffStop.stopOrder > 0) {
            "Dropoff stop order must be positive"
        }

        require(command.pickupStop.stopOrder < command.dropoffStop.stopOrder) {
            "Pickup stop order (${command.pickupStop.stopOrder}) must be before dropoff stop order (${command.dropoffStop.stopOrder})"
        }

        require(command.dropoffStop.stopOrder <= existingStopsCount + 2) {
            "Stop orders exceed available positions. Max: ${existingStopsCount + 2}"
        }
    }
}