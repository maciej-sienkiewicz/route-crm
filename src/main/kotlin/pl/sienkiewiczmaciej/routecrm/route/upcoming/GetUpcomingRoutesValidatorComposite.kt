// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/upcoming/GetUpcomingRoutesValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.upcoming

import org.springframework.stereotype.Component

@Component
class GetUpcomingRoutesValidatorComposite(
    private val contextBuilder: GetUpcomingRoutesContextBuilder,
    private val scheduleActiveValidator: UpcomingRoutesScheduleActiveValidator
) {
    suspend fun validate(query: GetUpcomingRoutesQuery): GetUpcomingRoutesValidationContext {
        // 1. Build context
        val context = contextBuilder.build(query)

        // 2. Run validators
        scheduleActiveValidator.validate(context)

        // 3. Return context
        return context
    }
}

@Component
class UpcomingRoutesScheduleActiveValidator {
    fun validate(context: GetUpcomingRoutesValidationContext) {
        require(context.schedule.active) {
            "Cannot view upcoming routes for inactive schedule ${context.schedule.id.value}"
        }
    }
}