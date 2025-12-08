package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface DailyPassengerMetricsJpaRepository : JpaRepository<DailyPassengerMetricsEntity, Long> {
    fun findByCompanyIdAndMetricDate(companyId: String, metricDate: LocalDate): DailyPassengerMetricsEntity?

    @Query("""
        SELECT m FROM DailyPassengerMetricsEntity m
        WHERE m.companyId = :companyId
AND m.metricDate >= :startDate
AND m.metricDate <= :endDate
ORDER BY m.metricDate ASC
""")
    fun findByCompanyIdAndMetricDateBetween(
        @Param("companyId") companyId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DailyPassengerMetricsEntity>
    @Modifying
    @Query("DELETE FROM DailyPassengerMetricsEntity m WHERE m.companyId = :companyId AND m.metricDate = :metricDate")
    fun deleteByCompanyIdAndMetricDate(
        @Param("companyId") companyId: String,
        @Param("metricDate") metricDate: LocalDate
    )
}
