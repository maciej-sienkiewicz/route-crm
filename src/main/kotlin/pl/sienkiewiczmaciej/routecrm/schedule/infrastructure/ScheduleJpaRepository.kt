package pl.sienkiewiczmaciej.routecrm.schedule.infrastructure

import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/schedule/infrastructure/ScheduleJpaRepository.kt

interface ScheduleJpaRepository : JpaRepository<ScheduleEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): ScheduleEntity?

    fun findByCompanyIdAndChildId(companyId: String, childId: String): List<ScheduleEntity>

    @Query("""
        SELECT COUNT(s) FROM ScheduleEntity s 
        WHERE s.companyId = :companyId 
        AND s.childId = :childId 
        AND s.active = true
    """)
    fun countActiveByCompanyIdAndChildId(companyId: String, childId: String): Int

    fun deleteByIdAndCompanyId(id: String, companyId: String)

    /**
     * Finds active schedules that are not assigned to any route on the given date
     * and are not covered by any absence.
     *
     * Business logic:
     * - Schedule must be active
     * - Schedule must be configured for the given day of week
     * - Schedule must NOT have a RouteStop on the given date
     * - Child must NOT have an active absence covering this date and schedule
     */
    @Query(value = """
        SELECT s.* FROM schedules s
        WHERE s.company_id = :companyId
        AND s.active = true
        AND jsonb_exists(s.days::jsonb, :dayOfWeek)
        AND NOT EXISTS (
            SELECT 1 FROM route_stops rs
            JOIN routes r ON r.id = rs.route_id AND r.company_id = rs.company_id
            WHERE rs.company_id = :companyId
            AND rs.schedule_id = s.id
            AND r.date = :date
            AND rs.is_cancelled = false
        )
        AND NOT EXISTS (
            SELECT 1 FROM child_absences ca
            WHERE ca.company_id = :companyId
            AND ca.child_id = s.child_id
            AND ca.start_date <= :date
            AND ca.end_date >= :date
            AND ca.status IN ('PLANNED', 'ACTIVE')
            AND (
                ca.absence_type = 'FULL_DAY'
                OR (ca.absence_type = 'SPECIFIC_SCHEDULE' AND ca.schedule_id = s.id)
            )
        )
        ORDER BY s.child_id, s.pickup_time
    """, nativeQuery = true)
    fun findUnassignedSchedulesForDate(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate,
        @Param("dayOfWeek") dayOfWeek: String
    ): List<ScheduleEntity>
}