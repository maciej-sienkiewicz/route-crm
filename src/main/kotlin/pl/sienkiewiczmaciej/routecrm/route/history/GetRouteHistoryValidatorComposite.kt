// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/history/GetRouteHistoryValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.history

import org.springframework.stereotype.Component

@Component
class GetRouteHistoryValidatorComposite(
    private val contextBuilder: GetRouteHistoryContextBuilder,
    private val scheduleActiveValidator: RouteHistoryScheduleActiveValidator
) {
    suspend fun validate(query: GetRouteHistoryQuery): GetRouteHistoryValidationContext {
        // 1. Build context
        val context = contextBuilder.build(query)

        // 2. Run validators (można dodać więcej w przyszłości)
        scheduleActiveValidator.validate(context)

        // 3. Return context
        return context
    }
}

@Component
class RouteHistoryScheduleActiveValidator {
    fun validate(context: GetRouteHistoryValidationContext) {
        // Schedule może być nieaktywne, ale chcemy je sprawdzić czy istnieje
        // Dodatkowe walidacje można tutaj umieścić w przyszłości
    }
}