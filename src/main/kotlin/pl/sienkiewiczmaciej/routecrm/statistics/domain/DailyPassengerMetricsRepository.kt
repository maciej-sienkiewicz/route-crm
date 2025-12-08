package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface DailyPassengerMetricsRepository {
    suspend fun save(metrics: DailyPassengerMetrics): DailyPassengerMetrics
    suspend fun findByCompanyAndDate(companyId: CompanyId, date: LocalDate): DailyPassengerMetrics?
    suspend fun findByCompanyAndDateRange(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyPassengerMetrics>
    suspend fun deleteByCompanyAndDate(companyId: CompanyId, date: LocalDate)
}