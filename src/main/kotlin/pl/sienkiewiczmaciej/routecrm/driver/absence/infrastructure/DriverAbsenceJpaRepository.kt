// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/infrastructure/DriverAbsenceJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import java.time.LocalDate

interface DriverAbsenceJpaRepository : JpaRepository<DriverAbsenceEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): DriverAbsenceEntity?

    @Query("""
        SELECT a FROM DriverAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.driverId = :driverId
        AND (CAST(:from AS date) IS NULL OR a.endDate >= :from)
        AND (CAST(:to AS date) IS NULL OR a.startDate <= :to)
        AND (:statuses IS NULL OR a.status IN :statuses)
        ORDER BY a.startDate DESC, a.createdAt DESC
    """)
    fun findByDriverAndDateRange(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?,
        @Param("statuses") statuses: Set<DriverAbsenceStatus>?
    ): List<DriverAbsenceEntity>

    @Query("""
        SELECT a FROM DriverAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.driverId = :driverId
        AND a.startDate <= :date
        AND a.endDate >= :date
        AND a.status IN ('PLANNED', 'ACTIVE')
    """)
    fun findActiveForDriverOnDate(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("date") date: LocalDate
    ): List<DriverAbsenceEntity>

    @Query("""
        SELECT COUNT(a) FROM DriverAbsenceEntity a
        WHERE a.companyId = :companyId
        AND a.driverId = :driverId
        AND a.startDate <= :to
        AND a.endDate >= :from
        AND (:statuses IS NULL OR a.status IN :statuses)
    """)
    fun countByDriverAndDateRange(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
        @Param("statuses") statuses: Set<DriverAbsenceStatus>?
    ): Int
}