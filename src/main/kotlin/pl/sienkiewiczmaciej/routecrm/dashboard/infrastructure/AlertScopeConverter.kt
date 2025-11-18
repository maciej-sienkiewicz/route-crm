// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/infrastructure/AlertScopeConverter.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.infrastructure

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.AlertScope

@Component
class AlertScopeConverter : Converter<String, AlertScope> {
    override fun convert(source: String): AlertScope {
        return when (source.uppercase().replace("_", "")) {
            "TOMORROW" -> AlertScope.TOMORROW
            "3DAYS", "THREEDAYS" -> AlertScope.THREE_DAYS
            "7DAYS", "SEVENDAYS" -> AlertScope.SEVEN_DAYS
            "30DAYS", "THIRTYDAYS" -> AlertScope.THIRTY_DAYS
            else -> throw IllegalArgumentException("Invalid AlertScope value: $source. Valid values: TOMORROW, THREE_DAYS, SEVEN_DAYS, THIRTY_DAYS")
        }
    }
}