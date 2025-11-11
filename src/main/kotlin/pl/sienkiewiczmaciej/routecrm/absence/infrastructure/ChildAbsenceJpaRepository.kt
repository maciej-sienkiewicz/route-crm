// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/infrastructure/ChildAbsenceJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.absence.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import java.time.LocalDate

interface ChildAbsenceJpaRepository : JpaRepository<ChildAbsenceEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): ChildAbsenceEntity?

    @Query("""
        SELECT a FROM ChildAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.childId = :childId
        AND (CAST(:from AS date) IS NULL OR a.endDate >= :from)
        AND (CAST(:to AS date) IS NULL OR a.startDate <= :to)
        AND (:statuses IS NULL OR a.status IN :statuses)
        ORDER BY a.startDate DESC, a.createdAt DESC
    """)
    fun findByChildAndDateRange(
        @Param("companyId") companyId: String,
        @Param("childId") childId: String,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?,
        @Param("statuses") statuses: Set<AbsenceStatus>?
    ): List<ChildAbsenceEntity>

    @Query("""
        SELECT a FROM ChildAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.scheduleId = :scheduleId
        AND (CAST(:from AS date) IS NULL OR a.endDate >= :from)
        AND (CAST(:to AS date) IS NULL OR a.startDate <= :to)
        ORDER BY a.startDate DESC
    """)
    fun findByScheduleAndDateRange(
        @Param("companyId") companyId: String,
        @Param("scheduleId") scheduleId: String,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?
    ): List<ChildAbsenceEntity>

    @Query("""
        SELECT a FROM ChildAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.childId = :childId
        AND a.startDate <= :date
        AND a.endDate >= :date
        AND a.status IN ('PLANNED', 'ACTIVE')
    """)
    fun findActiveForChildOnDate(
        @Param("companyId") companyId: String,
        @Param("childId") childId: String,
        @Param("date") date: LocalDate
    ): List<ChildAbsenceEntity>

    @Query("""
        SELECT a FROM ChildAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.childId = :childId
        AND a.scheduleId = :scheduleId
        AND a.startDate <= :date
        AND a.endDate >= :date
        AND a.status IN ('PLANNED', 'ACTIVE')
    """)
    fun findActiveForScheduleOnDate(
        @Param("companyId") companyId: String,
        @Param("childId") childId: String,
        @Param("scheduleId") scheduleId: String,
        @Param("date") date: LocalDate
    ): List<ChildAbsenceEntity>

    @Query("""
        SELECT COUNT(a) FROM ChildAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.childId = :childId
        AND a.startDate <= :to
        AND a.endDate >= :from
        AND (:statuses IS NULL OR a.status IN :statuses)
    """)
    fun countByChildAndDateRange(
        @Param("companyId") companyId: String,
        @Param("childId") childId: String,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
        @Param("statuses") statuses: Set<AbsenceStatus>?
    ): Int
}