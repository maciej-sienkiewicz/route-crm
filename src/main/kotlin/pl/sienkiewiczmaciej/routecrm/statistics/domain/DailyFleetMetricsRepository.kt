package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface DailyFleetMetricsRepository {
    suspend fun save(metrics: DailyFleetMetrics): DailyFleetMetrics
    suspend fun findByCompanyAndDate(companyId: CompanyId, date: LocalDate): DailyFleetMetrics?
    suspend fun findByCompanyAndDateRange(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyFleetMetrics>
    suspend fun deleteByCompanyAndDate(companyId: CompanyId, date: LocalDate)
}