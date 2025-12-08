package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface DailyCapacityMetricsRepository {
    suspend fun save(metrics: DailyCapacityMetrics): DailyCapacityMetrics
    suspend fun findByCompanyAndDate(companyId: CompanyId, date: LocalDate): DailyCapacityMetrics?
    suspend fun findByCompanyAndDateRange(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyCapacityMetrics>
    suspend fun deleteByCompanyAndDate(companyId: CompanyId, date: LocalDate)
}