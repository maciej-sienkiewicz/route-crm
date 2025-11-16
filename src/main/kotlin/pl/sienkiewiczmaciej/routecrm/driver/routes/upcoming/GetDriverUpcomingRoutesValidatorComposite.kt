// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/routes/upcoming/GetDriverUpcomingRoutesValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.driver.routes.upcoming

import org.springframework.stereotype.Component

@Component
class GetDriverUpcomingRoutesValidatorComposite(
    private val contextBuilder: GetDriverUpcomingRoutesContextBuilder
) {
    suspend fun validate(query: GetDriverUpcomingRoutesQuery): GetDriverUpcomingRoutesValidationContext {
        val context = contextBuilder.build(query)
        return context
    }
}