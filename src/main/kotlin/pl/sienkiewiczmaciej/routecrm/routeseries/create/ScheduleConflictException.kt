// routeseries/create/ScheduleConflictException.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.create

import java.time.LocalDate

/**
 * Exception thrown when schedules have conflicts with existing routes or series.
 * Separates conflicts into single routes and route series.
 */
class ScheduleConflictException(
    val singleRouteConflicts: Map<String, List<String>>,
    val seriesConflicts: Map<String, String>
) : RuntimeException("Schedule conflicts detected") {

    /**
     * Returns structured conflict data.
     * singleRoutes: Map of child name to list of conflict dates
     * series: Map of child name to series name
     */
    fun getConflictData(): ConflictData = ConflictData(
        singleRoutes = singleRouteConflicts,
        series = seriesConflicts
    )
}

data class ConflictData(
    val singleRoutes: Map<String, List<String>>,
    val series: Map<String, String>
)

/**
 * Data class for collecting conflict information before creating exception.
 */
data class ScheduleConflictInfo(
    val childName: String,
    val scheduleId: String,
    val conflictDate: LocalDate,
    val conflictType: ConflictType,
    val conflictDetails: String
)

enum class ConflictType {
    ALREADY_IN_ROUTE,
    ALREADY_IN_SERIES
}