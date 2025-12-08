package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface DriverDailyPerformanceJpaRepository : JpaRepository<DriverDailyPerformanceEntity, Long> {
    fun findByCompanyIdAndDriverIdAndMetricDate(
        companyId: String,
        driverId: String,
        metricDate: LocalDate
    ): DriverDailyPerformanceEntity?

    @Query("""
        SELECT p FROM DriverDailyPerformanceEntity p
        WHERE p.companyId = :companyId
        AND p.metricDate >= :startDate
        AND p.metricDate <= :endDate
        ORDER BY p.metricDate ASC, p.driverId ASC
    """)
    fun findByCompanyIdAndMetricDateBetween(
        @Param("companyId") companyId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DriverDailyPerformanceEntity>

    @Query("""
        SELECT p FROM DriverDailyPerformanceEntity p
        WHERE p.companyId = :companyId
        AND p.driverId = :driverId
        AND p.metricDate >= :startDate
        AND p.metricDate <= :endDate
        ORDER BY p.metricDate ASC
    """)
    fun findByCompanyIdAndDriverIdAndMetricDateBetween(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DriverDailyPerformanceEntity>

    @Modifying
    @Query("DELETE FROM DriverDailyPerformanceEntity p WHERE p.companyId = :companyId AND p.metricDate = :metricDate")
    fun deleteByCompanyIdAndMetricDate(
        @Param("companyId") companyId: String,
        @Param("metricDate") metricDate: LocalDate
    )
}