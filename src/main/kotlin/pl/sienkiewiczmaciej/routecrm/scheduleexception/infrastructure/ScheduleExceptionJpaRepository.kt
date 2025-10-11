package pl.sienkiewiczmaciej.routecrm.scheduleexception.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ScheduleExceptionJpaRepository : JpaRepository<ScheduleExceptionEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): ScheduleExceptionEntity?

    @Query("""
        SELECT e FROM ScheduleExceptionEntity e
        WHERE e.companyId = :companyId
        AND e.scheduleId = :scheduleId
        AND (:from IS NULL OR e.exceptionDate >= :from)
        AND (:to IS NULL OR e.exceptionDate <= :to)
        ORDER BY e.exceptionDate ASC
    """)
    fun findByScheduleIdAndDateRange(
        companyId: String,
        scheduleId: String,
        from: LocalDate?,
        to: LocalDate?
    ): List<ScheduleExceptionEntity>

    @Query("""
        SELECT e FROM ScheduleExceptionEntity e
        WHERE e.companyId = :companyId
        AND e.childId = :childId
        AND (:from IS NULL OR e.exceptionDate >= :from)
        AND (:to IS NULL OR e.exceptionDate <= :to)
        ORDER BY e.exceptionDate ASC
    """)
    fun findByChildIdAndDateRange(
        companyId: String,
        childId: String,
        from: LocalDate?,
        to: LocalDate?
    ): List<ScheduleExceptionEntity>

    fun existsByCompanyIdAndScheduleIdAndExceptionDate(
        companyId: String,
        scheduleId: String,
        exceptionDate: LocalDate
    ): Boolean

    @Query("""
        SELECT COUNT(e) FROM ScheduleExceptionEntity e
        WHERE e.companyId = :companyId
        AND e.childId = :childId
        AND e.exceptionDate >= :from
        AND e.exceptionDate <= :to
    """)
    fun countByChildIdAndDateRange(
        companyId: String,
        childId: String,
        from: LocalDate,
        to: LocalDate
    ): Int

    fun deleteByIdAndCompanyId(id: String, companyId: String)
}