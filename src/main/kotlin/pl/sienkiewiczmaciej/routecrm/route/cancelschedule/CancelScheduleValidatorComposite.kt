// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/cancelschedule/CancelScheduleValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.cancelschedule

import org.springframework.stereotype.Component

/**
 * Composite validator for CancelRouteSchedule operation.
 */
@Component
class CancelScheduleValidatorComposite(
    private val contextBuilder: CancelScheduleContextBuilder,
    private val scheduleExistsValidator: CancelScheduleExistsValidator,
    private val scheduleHasTwoStopsValidator: CancelScheduleHasTwoStopsValidator,
    private val stopsNotExecutedValidator: CancelScheduleStopsNotExecutedValidator,
    private val stopsNotCancelledValidator: CancelScheduleStopsNotCancelledValidator
) {
    suspend fun validate(command: CancelRouteScheduleCommand): CancelScheduleValidationContext {
        // 1. Build context
        val context = contextBuilder.build(command)

        // 2. Run validators
        scheduleExistsValidator.validate(context)
        scheduleHasTwoStopsValidator.validate(command, context)
        stopsNotExecutedValidator.validate(context)
        stopsNotCancelledValidator.validate(command, context)

        // 3. Return context
        return context
    }
}

@Component
class CancelScheduleExistsValidator {
    fun validate(context: CancelScheduleValidationContext) {
        require(context.scheduleStops.isNotEmpty()) {
            "Schedule not found in route"
        }
    }
}

@Component
class CancelScheduleHasTwoStopsValidator {
    fun validate(command: CancelRouteScheduleCommand, context: CancelScheduleValidationContext) {
        require(context.scheduleStops.size == 2) {
            "Expected 2 stops (pickup and dropoff) for schedule ${command.scheduleId.value}, found ${context.scheduleStops.size}"
        }
    }
}

@Component
class CancelScheduleStopsNotExecutedValidator {
    fun validate(context: CancelScheduleValidationContext) {
        require(context.scheduleStops.none { it.isExecuted() }) {
            "Cannot cancel schedule: one or both stops have already been executed"
        }
    }
}

@Component
class CancelScheduleStopsNotCancelledValidator {
    fun validate(command: CancelRouteScheduleCommand, context: CancelScheduleValidationContext) {
        require(context.scheduleStops.none { it.isCancelled }) {
            "Schedule ${command.scheduleId.value} is already cancelled"
        }
    }
}