// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/deleteschedule/DeleteScheduleValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.deleteschedule

import org.springframework.stereotype.Component

/**
 * Composite validator for DeleteRouteSchedule operation.
 */
@Component
class DeleteScheduleValidatorComposite(
    private val contextBuilder: DeleteScheduleContextBuilder,
    private val routeStatusValidator: DeleteScheduleRouteStatusValidator,
    private val scheduleExistsValidator: DeleteScheduleExistsValidator,
    private val scheduleHasTwoStopsValidator: DeleteScheduleHasTwoStopsValidator,
    private val stopsNotExecutedValidator: DeleteScheduleStopsNotExecutedValidator
) {
    suspend fun validate(command: DeleteRouteScheduleCommand): DeleteScheduleValidationContext {
        // 1. Build context
        val context = contextBuilder.build(command)

        // 2. Run validators
        routeStatusValidator.validate(context)
        scheduleExistsValidator.validate(context)
        scheduleHasTwoStopsValidator.validate(command, context)
        stopsNotExecutedValidator.validate(command, context)

        // 3. Return context
        return context
    }
}

@Component
class DeleteScheduleRouteStatusValidator {
    fun validate(context: DeleteScheduleValidationContext) {
        require(context.route.canDeleteStops()) {
            "Cannot delete schedule from route with status ${context.route.status}"
        }
    }
}

@Component
class DeleteScheduleExistsValidator {
    fun validate(context: DeleteScheduleValidationContext) {
        require(context.scheduleStops.isNotEmpty()) {
            "Schedule not found in route"
        }
    }
}

@Component
class DeleteScheduleHasTwoStopsValidator {
    fun validate(command: DeleteRouteScheduleCommand, context: DeleteScheduleValidationContext) {
        require(context.scheduleStops.size == 2) {
            "Expected 2 stops (pickup and dropoff) for schedule ${command.scheduleId.value}, found ${context.scheduleStops.size}"
        }
    }
}

@Component
class DeleteScheduleStopsNotExecutedValidator {
    fun validate(command: DeleteRouteScheduleCommand, context: DeleteScheduleValidationContext) {
        context.scheduleStops.forEach { stop ->
            require(stop.canBeDeleted()) {
                "Cannot delete schedule: stop ${stop.id.value} (${stop.stopType}) has already been executed"
            }
        }
    }
}