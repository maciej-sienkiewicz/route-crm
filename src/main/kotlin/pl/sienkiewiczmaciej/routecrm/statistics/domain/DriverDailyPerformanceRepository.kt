package pl.sienkiewiczmaciej.routecrm.statistics.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface DriverDailyPerformanceRepository {
    suspend fun save(performance: DriverDailyPerformance): DriverDailyPerformance
    suspend fun findByCompanyDriverAndDate(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate
    ): DriverDailyPerformance?
    suspend fun findByCompanyAndDateRange(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DriverDailyPerformance>
    suspend fun findByDriverAndDateRange(
        companyId: CompanyId,
        driverId: DriverId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DriverDailyPerformance>
    suspend fun deleteByCompanyAndDate(companyId: CompanyId, date: LocalDate)
}