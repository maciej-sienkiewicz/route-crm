package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface DailyWorkloadMetricsRepository {
    suspend fun save(metrics: DailyWorkloadMetrics): DailyWorkloadMetrics
    suspend fun findByCompanyAndDate(companyId: CompanyId, date: LocalDate): DailyWorkloadMetrics?
    suspend fun findByCompanyAndDateRange(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyWorkloadMetrics>
    suspend fun deleteByCompanyAndDate(companyId: CompanyId, date: LocalDate)
}