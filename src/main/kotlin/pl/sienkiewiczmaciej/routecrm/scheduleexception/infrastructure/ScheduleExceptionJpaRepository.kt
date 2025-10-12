package pl.sienkiewiczmaciej.routecrm.scheduleexception.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ScheduleExceptionJpaRepository : JpaRepository<ScheduleExceptionEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): ScheduleExceptionEntity?

    @Query("""
        SELECT e FROM ScheduleExceptionEntity e
        WHERE e.companyId = :companyId
        AND e.scheduleId = :scheduleId
        AND (CAST(:from AS date) IS NULL OR e.exceptionDate >= :from)
        AND (CAST(:to AS date) IS NULL OR e.exceptionDate <= :to)
        ORDER BY e.exceptionDate ASC
    """)
    fun findByScheduleIdAndDateRange(
        @Param("companyId") companyId: String,
        @Param("scheduleId") scheduleId: String,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?
    ): List<ScheduleExceptionEntity>

    @Query("""
        SELECT e FROM ScheduleExceptionEntity e
        WHERE e.companyId = :companyId
        AND e.childId = :childId
        AND (CAST(:from AS date) IS NULL OR e.exceptionDate >= :from)
        AND (CAST(:to AS date) IS NULL OR e.exceptionDate <= :to)
        ORDER BY e.exceptionDate ASC
    """)
    fun findByChildIdAndDateRange(
        @Param("companyId") companyId: String,
        @Param("childId") childId: String,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?
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