// routeseries/domain/services/SeriesConflictResolver.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.domain.services

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesSchedule
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

sealed class ConflictResolution {
    data class NoConflict(
        val effectiveFrom: LocalDate,
        val effectiveTo: LocalDate?
    ) : ConflictResolution()

    data class Conflict(
        val requestedFrom: LocalDate,
        val limitedTo: LocalDate,
        val reason: String,
        val existingSchedule: RouteSeriesSchedule
    ) : ConflictResolution()
}

@Component
class SeriesConflictResolver(
    private val scheduleRepository: RouteSeriesScheduleRepository
) {

    suspend fun resolveAddChildConflict(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        scheduleId: ScheduleId,
        requestedFrom: LocalDate
    ): ConflictResolution {

        val existingSchedule = scheduleRepository.findBySeriesAndSchedule(
            companyId = companyId,
            seriesId = seriesId,
            scheduleId = scheduleId
        )

        if (existingSchedule == null) {
            return ConflictResolution.NoConflict(
                effectiveFrom = requestedFrom,
                effectiveTo = null
            )
        }

        if (requestedFrom >= existingSchedule.validFrom) {
            return ConflictResolution.NoConflict(
                effectiveFrom = requestedFrom,
                effectiveTo = null
            )
        }

        val limitedTo = existingSchedule.validFrom.minusDays(1)

        return ConflictResolution.Conflict(
            requestedFrom = requestedFrom,
            limitedTo = limitedTo,
            reason = "Schedule already in series from ${existingSchedule.validFrom}",
            existingSchedule = existingSchedule
        )
    }
}