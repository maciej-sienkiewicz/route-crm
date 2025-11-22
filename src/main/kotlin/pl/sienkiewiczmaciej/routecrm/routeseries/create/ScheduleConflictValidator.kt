// routeseries/create/ScheduleConflictValidator.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.create

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Component
class ScheduleConflictValidator(
    private val stopRepository: RouteStopRepository,
    private val routeRepository: RouteRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val seriesRepository: RouteSeriesRepository,
    private val scheduleRepository: ScheduleRepository,
    private val childRepository: ChildRepository
) {

    /**
     * Validates if schedules are available for creating a route series.
     *
     * @throws ScheduleConflictException with separated conflicts:
     *   - singleRoutes: Map<"Jan Kowalski", List<"2024-01-15">>
     *   - series: Map<"Jan Kowalski", "Trasa Poniedziałkowa">
     */
    suspend fun validateAndThrowIfConflicts(
        companyId: CompanyId,
        scheduleIds: List<ScheduleId>,
        startDate: LocalDate,
        endDate: LocalDate?,
        recurrenceInterval: Int,
        dayOfWeek: java.time.DayOfWeek
    ) {
        val conflictInfos = validateSchedulesAvailability(
            companyId = companyId,
            scheduleIds = scheduleIds,
            startDate = startDate,
            endDate = endDate,
            recurrenceInterval = recurrenceInterval,
            dayOfWeek = dayOfWeek
        )

        if (conflictInfos.isNotEmpty()) {
            val childrenMap = loadChildrenForSchedules(companyId, scheduleIds)

            // Separate conflicts by type
            val routeConflicts = conflictInfos.filter { it.conflictType == ConflictType.ALREADY_IN_ROUTE }
            val seriesConflictsInfo = conflictInfos.filter { it.conflictType == ConflictType.ALREADY_IN_SERIES }

            // Build single route conflicts map: child name -> dates
            val singleRouteConflicts = if (routeConflicts.isNotEmpty()) {
                routeConflicts
                    .groupBy { info ->
                        val scheduleId = ScheduleId.from(info.scheduleId)
                        val child = childrenMap[scheduleId]
                        child?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown Child"
                    }
                    .mapValues { (_, infos) ->
                        infos.map { it.conflictDate.toString() }
                            .distinct()
                            .sorted()
                    }
            } else {
                emptyMap()
            }

            // Build series conflicts map: child name -> series name
            val seriesConflicts = if (seriesConflictsInfo.isNotEmpty()) {
                val seriesMap = mutableMapOf<String, String>()

                for (info in seriesConflictsInfo) {
                    val scheduleId = ScheduleId.from(info.scheduleId)
                    val child = childrenMap[scheduleId]
                    val childName = child?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown Child"

                    // Extract series ID from conflict details
                    val match = "Series ([A-Z]+-[a-f0-9-]+)".toRegex().find(info.conflictDetails)
                    val seriesIdStr = match?.groupValues?.get(1)

                    if (seriesIdStr != null) {
                        val seriesId = RouteSeriesId.from(seriesIdStr)
                        val series = seriesRepository.findById(companyId, seriesId)
                        if (series != null) {
                            seriesMap[childName] = series.seriesName
                        }
                    }
                }

                seriesMap
            } else {
                emptyMap()
            }

            throw ScheduleConflictException(
                singleRouteConflicts = singleRouteConflicts,
                seriesConflicts = seriesConflicts
            )
        }
    }

    private suspend fun validateSchedulesAvailability(
        companyId: CompanyId,
        scheduleIds: List<ScheduleId>,
        startDate: LocalDate,
        endDate: LocalDate?,
        recurrenceInterval: Int,
        dayOfWeek: java.time.DayOfWeek
    ): List<ScheduleConflictInfo> {
        val conflicts = mutableListOf<ScheduleConflictInfo>()

        val occurrenceDates = generateOccurrenceDates(
            startDate,
            endDate,
            recurrenceInterval,
            dayOfWeek
        )

        for (scheduleId in scheduleIds) {
            val routeConflicts = checkRouteConflicts(
                companyId,
                scheduleId,
                occurrenceDates
            )
            conflicts.addAll(routeConflicts)

            val seriesConflicts = checkSeriesConflicts(
                companyId,
                scheduleId,
                occurrenceDates
            )
            conflicts.addAll(seriesConflicts)
        }

        return conflicts
    }

    private fun generateOccurrenceDates(
        startDate: LocalDate,
        endDate: LocalDate?,
        recurrenceInterval: Int,
        dayOfWeek: java.time.DayOfWeek
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        val effectiveEndDate = endDate ?: startDate.plusYears(1)

        while (!currentDate.isAfter(effectiveEndDate)) {
            if (currentDate.dayOfWeek == dayOfWeek) {
                val weeksSinceStart = java.time.temporal.ChronoUnit.WEEKS.between(startDate, currentDate)
                if (weeksSinceStart % recurrenceInterval == 0L) {
                    dates.add(currentDate)
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        return dates
    }

    private suspend fun checkRouteConflicts(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        occurrenceDates: List<LocalDate>
    ): List<ScheduleConflictInfo> {
        val conflicts = mutableListOf<ScheduleConflictInfo>()

        for (date in occurrenceDates) {
            val existingStops = stopRepository.findByScheduleAndDate(
                companyId = companyId,
                scheduleId = scheduleId,
                date = date
            )

            // Filter only stops from routes WITHOUT series (single routes)
            for (stop in existingStops) {
                val route = routeRepository.findById(companyId, stop.routeId)
                if (route != null && route.seriesId == null) {
                    conflicts.add(
                        ScheduleConflictInfo(
                            childName = "",
                            scheduleId = scheduleId.value,
                            conflictDate = date,
                            conflictType = ConflictType.ALREADY_IN_ROUTE,
                            conflictDetails = "Route ${route.id.value}"
                        )
                    )
                    break // One conflict per date is enough
                }
            }
        }

        return conflicts
    }

    private suspend fun checkSeriesConflicts(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        occurrenceDates: List<LocalDate>
    ): List<ScheduleConflictInfo> {
        val conflicts = mutableListOf<ScheduleConflictInfo>()

        val existingSeriesSchedules = seriesScheduleRepository.findBySchedule(
            companyId = companyId,
            scheduleId = scheduleId
        )

        if (existingSeriesSchedules.isNotEmpty()) {
            // Check if any occurrence date falls within any series schedule validity
            val hasConflict = occurrenceDates.any { date ->
                existingSeriesSchedules.any { it.isActiveOn(date) }
            }

            if (hasConflict) {
                val seriesId = existingSeriesSchedules.first().seriesId
                conflicts.add(
                    ScheduleConflictInfo(
                        childName = "",
                        scheduleId = scheduleId.value,
                        conflictDate = occurrenceDates.first(), // Just for reference
                        conflictType = ConflictType.ALREADY_IN_SERIES,
                        conflictDetails = "Series ${seriesId.value}"
                    )
                )
            }
        }

        return conflicts
    }

    private suspend fun loadChildrenForSchedules(
        companyId: CompanyId,
        scheduleIds: List<ScheduleId>
    ): Map<ScheduleId, Child> {
        val childrenMap = mutableMapOf<ScheduleId, Child>()

        for (scheduleId in scheduleIds) {
            val schedule = scheduleRepository.findById(companyId, scheduleId)
            if (schedule != null) {
                val child = childRepository.findById(companyId, schedule.childId)
                if (child != null) {
                    childrenMap[scheduleId] = child
                }
            }
        }

        return childrenMap
    }
}