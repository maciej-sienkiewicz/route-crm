// routeseries/domain/services/RouteSeriesMaterializationService.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.domain.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.absence.domain.services.AbsenceConflictChecker
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.route.domain.services.GapBasedStopOrderCalculator
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate

data class MaterializationResult(
    val routesCreated: Int,
    val routesSkipped: Int,
    val routesUpdated: Int,
    val dateRange: ClosedRange<LocalDate>
)

sealed class MaterializationOutcome {
    data class Created(val routeId: RouteId) : MaterializationOutcome()
    data class Updated(val routeId: RouteId) : MaterializationOutcome()
    data class Skipped(val routeId: RouteId?) : MaterializationOutcome()
}

@Service
class RouteSeriesMaterializationService(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val scheduleRepository: ScheduleRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val occurrenceRepository: RouteSeriesOccurrenceRepository,
    private val absenceConflictChecker: AbsenceConflictChecker,
    private val gapBasedStopOrderCalculator: GapBasedStopOrderCalculator,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    suspend fun materializeForDateRange(
        companyId: CompanyId,
        dateRange: ClosedRange<LocalDate>,
        forceRegenerate: Boolean = false
    ): MaterializationResult {
        logger.info("Materializing routes for company ${companyId.value} in range $dateRange")

        var created = 0
        var skipped = 0
        var updated = 0

        val activeSeries = routeSeriesRepository.findActive(
            companyId = companyId,
            startDate = dateRange.start,
            endDate = dateRange.endInclusive
        )

        logger.info("Found ${activeSeries.size} active series for company ${companyId.value}")

        for (series in activeSeries) {
            val occurrenceDates = calculateOccurrences(series, dateRange)

            for (date in occurrenceDates) {
                val result = materializeOccurrence(series, date, forceRegenerate)

                when (result) {
                    is MaterializationOutcome.Created -> created++
                    is MaterializationOutcome.Skipped -> skipped++
                    is MaterializationOutcome.Updated -> updated++
                }
            }
        }

        logger.info(
            "Materialization complete for ${companyId.value}: " +
                    "created=$created, skipped=$skipped, updated=$updated"
        )

        return MaterializationResult(created, skipped, updated, dateRange)
    }

    private fun calculateOccurrences(
        series: RouteSeries,
        dateRange: ClosedRange<LocalDate>
    ): List<LocalDate> {
        val occurrences = mutableListOf<LocalDate>()
        var currentDate = maxOf(series.startDate, dateRange.start)
        val endDate = listOfNotNull(
            series.endDate,
            dateRange.endInclusive
        ).minOrNull() ?: dateRange.endInclusive

        while (!currentDate.isAfter(endDate)) {
            if (series.matchesRecurrencePattern(currentDate)) {
                occurrences.add(currentDate)
            }
            currentDate = currentDate.plusDays(1)
        }

        return occurrences
    }

    private suspend fun materializeOccurrence(
        series: RouteSeries,
        date: LocalDate,
        forceRegenerate: Boolean
    ): MaterializationOutcome {
        val existing = occurrenceRepository.findBySeriesAndDate(
            series.companyId,
            series.id,
            date
        )

        if (existing != null && existing.status == OccurrenceStatus.MATERIALIZED) {
            if (!forceRegenerate) {
                return MaterializationOutcome.Skipped(existing.routeId)
            }
            if (existing.routeId != null) {
                routeRepository.delete(series.companyId, existing.routeId)
            }
        }

        // ===== POPRAWIONA LOGIKA =====
        // Pobierz schedules aktywne dla TEJ serii na dany dzień
        val seriesSchedules = seriesScheduleRepository.findActiveBySeries(
            companyId = series.companyId,
            seriesId = series.id,
            date = date
        )

        if (seriesSchedules.isEmpty()) {
            logger.debug("No schedules for series ${series.id.value} on $date")
            return MaterializationOutcome.Skipped(null)
        }

        // Sprawdź czy schedules nie są już zajęte w INNYCH trasach lub seriach
        val availableSchedules = seriesSchedules.filter { seriesSchedule ->
            val hasConflict = hasScheduleConflict(
                companyId = series.companyId,
                scheduleId = seriesSchedule.scheduleId,
                date = date,
                excludeSeriesId = series.id // Wykluczamy obecną serię z walidacji
            )
            !hasConflict
        }

        if (availableSchedules.isEmpty()) {
            logger.debug("All schedules have conflicts on $date for series ${series.id.value}")
            return MaterializationOutcome.Skipped(null)
        }

        // Filtruj schedules z nieobecnościami
        val schedulesWithoutAbsences = availableSchedules.filter { seriesSchedule ->
            val absenceConflicts = absenceConflictChecker.checkConflictsForSchedule(
                companyId = series.companyId,
                childId = seriesSchedule.childId,
                scheduleId = seriesSchedule.scheduleId,
                date = date
            )
            absenceConflicts.isEmpty()
        }

        if (schedulesWithoutAbsences.isEmpty()) {
            logger.debug("All available schedules have absences on $date for series ${series.id.value}")
            return MaterializationOutcome.Skipped(null)
        }
        // =============================

        val route = Route.create(
            companyId = series.companyId,
            routeName = series.formatRouteName(date),
            date = date,
            driverId = series.driverId,
            vehicleId = series.vehicleId,
            estimatedStartTime = series.estimatedStartTime,
            estimatedEndTime = series.estimatedEndTime,
            seriesId = series.id,
            seriesOccurrenceDate = date
        )

        val savedRoute = routeRepository.save(route)

        val stops = schedulesWithoutAbsences.flatMap { seriesSchedule ->
            val schedule = scheduleRepository.findById(
                series.companyId,
                seriesSchedule.scheduleId
            ) ?: return@flatMap emptyList()

            listOf(
                RouteStop.create(
                    companyId = series.companyId,
                    routeId = savedRoute.id,
                    stopOrder = seriesSchedule.pickupStopOrder,
                    stopType = StopType.PICKUP,
                    childId = seriesSchedule.childId,
                    scheduleId = seriesSchedule.scheduleId,
                    estimatedTime = schedule.pickupTime,
                    address = schedule.pickupAddress,
                    createdAt = Instant.now(),
                ),
                RouteStop.create(
                    companyId = series.companyId,
                    routeId = savedRoute.id,
                    stopOrder = seriesSchedule.dropoffStopOrder,
                    stopType = StopType.DROPOFF,
                    childId = seriesSchedule.childId,
                    scheduleId = seriesSchedule.scheduleId,
                    estimatedTime = schedule.dropoffTime,
                    address = schedule.dropoffAddress,
                    createdAt = Instant.now(),
                )
            )
        }.let { gapBasedStopOrderCalculator.rebalance(it) }

        stopRepository.saveAll(stops)

        occurrenceRepository.save(
            RouteSeriesOccurrence.materialized(
                companyId = series.companyId,
                seriesId = series.id,
                occurrenceDate = date,
                routeId = savedRoute.id
            )
        )

        return if (existing != null) {
            MaterializationOutcome.Updated(savedRoute.id)
        } else {
            MaterializationOutcome.Created(savedRoute.id)
        }
    }

    /**
     * Checks if a schedule is already assigned to any route or series on the given date,
     * excluding the specified series.
     */
    private suspend fun hasScheduleConflict(
        companyId: CompanyId,
        scheduleId: pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId,
        date: LocalDate,
        excludeSeriesId: RouteSeriesId
    ): Boolean {
        // Check if schedule has stops in any regular route or route from different series
        val existingStops = stopRepository.findByScheduleAndDate(
            companyId = companyId,
            scheduleId = scheduleId,
            date = date
        )

        // If stops exist, check if they belong to a different series or no series at all
        val hasConflict = existingStops.any { stop ->
            val route = routeRepository.findById(companyId, stop.routeId)
            route != null && route.seriesId != excludeSeriesId
        }

        return hasConflict
    }
}