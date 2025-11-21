// routeseries/removechild/RemoveChildValidators.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.removechild

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesSchedule
import java.time.LocalDate

@Component
class RemoveChildEffectiveDateValidator {
    fun validate(
        effectiveFrom: LocalDate,
        seriesSchedule: RouteSeriesSchedule
    ) {
        require(!effectiveFrom.isBefore(seriesSchedule.validFrom)) {
            "Cannot remove schedule before it was added (${seriesSchedule.validFrom})"
        }

        if (seriesSchedule.validTo != null) {
            require(!effectiveFrom.isAfter(seriesSchedule.validTo)) {
                "Schedule already removed on ${seriesSchedule.validTo}"
            }
        }
    }
}