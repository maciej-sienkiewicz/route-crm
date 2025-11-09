// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/TimeRange.kt
package pl.sienkiewiczmaciej.routecrm.route.domain

import java.time.LocalTime

/**
 * Value object representing a time range within a day.
 */
data class TimeRange(
    val start: LocalTime,
    val end: LocalTime
) {
    init {
        require(end.isAfter(start)) {
            "End time must be after start time"
        }
    }

    /**
     * Checks if this time range overlaps with another time range.
     * Two ranges overlap if one starts before the other ends.
     */
    fun overlapsWith(other: TimeRange): Boolean {
        return this.start < other.end && this.end > other.start
    }

    /**
     * Checks if this time range contains a specific time.
     */
    fun contains(time: LocalTime): Boolean {
        return !time.isBefore(start) && time.isBefore(end)
    }

    /**
     * Checks if this time range completely contains another time range.
     */
    fun contains(other: TimeRange): Boolean {
        return !other.start.isBefore(this.start) && !other.end.isAfter(this.end)
    }
}