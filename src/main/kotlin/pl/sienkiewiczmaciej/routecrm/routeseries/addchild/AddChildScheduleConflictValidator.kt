// routeseries/addchild/AddChildScheduleConflictValidator.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.create.ScheduleConflictException
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeries
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Component
class AddChildScheduleConflictValidator(
    private val stopRepository: RouteStopRepository,
    private val routeRepository: RouteRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val seriesRepository: RouteSeriesRepository
) {

    /**
     * Validates if schedule can be added to the series without conflicts.
     *
     * @throws ScheduleConflictException with separated conflicts:
     *   - singleRoutes: Map<"Jan Kowalski", List<"2024-01-15">>
     *   - series: Map<"Jan Kowalski", "Trasa Poniedziałkowa">
     */
    suspend fun validateAndThrowIfConflicts(
        companyId: CompanyId,
        series: RouteSeries,
        child: Child,
        scheduleId: ScheduleId,
        effectiveFrom: LocalDate,
        effectiveTo: LocalDate?
    ) {
        val affectedDates = generateAffectedDates(series, effectiveFrom, effectiveTo)

        val singleRouteConflictDates = mutableListOf<LocalDate>()
        val conflictingSeriesIds = mutableSetOf<RouteSeriesId>()

        for (date in affectedDates) {
            val conflictInfo = checkDateConflict(
                companyId = companyId,
                scheduleId = scheduleId,
                date = date,
                excludeSeriesId = series.id
            )

            when (conflictInfo) {
                is DateConflictInfo.SingleRouteConflict -> {
                    singleRouteConflictDates.add(date)
                }
                is DateConflictInfo.SeriesConflict -> {
                    conflictingSeriesIds.add(conflictInfo.seriesId)
                }
                DateConflictInfo.NoConflict -> { /* continue */ }
            }
        }

        if (singleRouteConflictDates.isNotEmpty() || conflictingSeriesIds.isNotEmpty()) {
            val childName = "${child.firstName} ${child.lastName}"

            val singleRouteConflicts = if (singleRouteConflictDates.isNotEmpty()) {
                mapOf(childName to singleRouteConflictDates.map { it.toString() }.sorted())
            } else {
                emptyMap()
            }

            val seriesConflicts = if (conflictingSeriesIds.isNotEmpty()) {
                // Load series name for the first conflicting series
                val firstSeriesId = conflictingSeriesIds.first()
                val seriesEntity = seriesRepository.findById(companyId, firstSeriesId)

                if (seriesEntity != null) {
                    mapOf(childName to seriesEntity.seriesName)
                } else {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            throw ScheduleConflictException(
                singleRouteConflicts = singleRouteConflicts,
                seriesConflicts = seriesConflicts
            )
        }
    }

    private fun generateAffectedDates(
        series: RouteSeries,
        effectiveFrom: LocalDate,
        effectiveTo: LocalDate?
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = maxOf(series.startDate, effectiveFrom)
        val endDate = listOfNotNull(
            series.endDate,
            effectiveTo
        ).minOrNull() ?: effectiveFrom.plusYears(1)

        while (!currentDate.isAfter(endDate)) {
            if (series.matchesRecurrencePattern(currentDate)) {
                dates.add(currentDate)
            }
            currentDate = currentDate.plusDays(1)
        }

        return dates
    }

    private suspend fun checkDateConflict(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        date: LocalDate,
        excludeSeriesId: RouteSeriesId
    ): DateConflictInfo {
        val existingStops = stopRepository.findByScheduleAndDate(
            companyId = companyId,
            scheduleId = scheduleId,
            date = date
        )

        if (existingStops.isEmpty()) {
            return DateConflictInfo.NoConflict
        }

        for (stop in existingStops) {
            val route = routeRepository.findById(companyId, stop.routeId) ?: continue

            when {
                route.seriesId == null -> {
                    // Conflict with single route (no series)
                    return DateConflictInfo.SingleRouteConflict
                }
                route.seriesId != excludeSeriesId -> {
                    // Conflict with different series
                    return DateConflictInfo.SeriesConflict(route.seriesId!!)
                }
            }
        }

        return DateConflictInfo.NoConflict
    }

    private sealed class DateConflictInfo {
        object NoConflict : DateConflictInfo()
        object SingleRouteConflict : DateConflictInfo()
        data class SeriesConflict(val seriesId: RouteSeriesId) : DateConflictInfo()
    }
}