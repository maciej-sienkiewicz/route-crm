// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/domain/services/SeriesConflictResolver.kt
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
        val effectiveTo: LocalDate?,
        val note: String? = null
    ) : ConflictResolution()

    data class Conflict(
        val requestedFrom: LocalDate,
        val limitedTo: LocalDate,
        val reason: String,
        val existingSchedule: RouteSeriesSchedule,
        val message: String
    ) : ConflictResolution() {
        companion object {
            fun create(
                requestedFrom: LocalDate,
                existingSchedule: RouteSeriesSchedule
            ): Conflict {
                val limitedTo = existingSchedule.validFrom.minusDays(1)
                val message = buildMessage(requestedFrom, limitedTo, existingSchedule)

                return Conflict(
                    requestedFrom = requestedFrom,
                    limitedTo = limitedTo,
                    reason = "Schedule already in series from ${existingSchedule.validFrom}",
                    existingSchedule = existingSchedule,
                    message = message
                )
            }

            private fun buildMessage(
                from: LocalDate,
                to: LocalDate,
                existing: RouteSeriesSchedule
            ): String {
                return "Od dnia ${existing.validFrom} pojawiły się zmiany. " +
                        "Możesz wprowadzić zmiany od $from do $to. " +
                        "Czy zaakceptować taką zmianę?"
            }
        }
    }
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
                effectiveTo = null,
                note = "Schedule already in series from ${existingSchedule.validFrom}"
            )
        }

        return ConflictResolution.Conflict.create(requestedFrom, existingSchedule)
    }

    suspend fun resolveRemoveChildConflict(
        companyId: CompanyId,
        seriesId: RouteSeriesId,
        scheduleId: ScheduleId,
        requestedFrom: LocalDate
    ): ConflictResolution {

        val existingSchedule = scheduleRepository.findBySeriesAndSchedule(
            companyId = companyId,
            seriesId = seriesId,
            scheduleId = scheduleId
        ) ?: throw IllegalArgumentException("Schedule not found in series")

        if (requestedFrom <= existingSchedule.validFrom) {
            throw IllegalArgumentException(
                "Cannot remove schedule before it was added (${existingSchedule.validFrom})"
            )
        }

        if (existingSchedule.validTo != null && requestedFrom > existingSchedule.validTo) {
            throw IllegalArgumentException(
                "Schedule already removed on ${existingSchedule.validTo}"
            )
        }

        return ConflictResolution.NoConflict(
            effectiveFrom = requestedFrom,
            effectiveTo = existingSchedule.validTo
        )
    }
}