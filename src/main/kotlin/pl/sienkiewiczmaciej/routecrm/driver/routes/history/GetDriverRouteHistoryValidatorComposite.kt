// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/history/GetDriverRouteHistoryValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes.history

import org.springframework.stereotype.Component

@Component
class GetDriverRouteHistoryValidatorComposite(
    private val contextBuilder: GetDriverRouteHistoryContextBuilder
) {
    suspend fun validate(query: GetDriverRouteHistoryQuery): GetDriverRouteHistoryValidationContext {
        val context = contextBuilder.build(query)
        return context
    }
}