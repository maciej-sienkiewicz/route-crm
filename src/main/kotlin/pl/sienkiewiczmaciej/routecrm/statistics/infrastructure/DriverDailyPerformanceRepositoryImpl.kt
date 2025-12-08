package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DriverDailyPerformance
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DriverDailyPerformanceRepository
import java.time.LocalDate

@Repository
class DriverDailyPerformanceRepositoryImpl(
    private val jpaRepository: DriverDailyPerformanceJpaRepository
) : DriverDailyPerformanceRepository {

    override suspend fun save(performance: DriverDailyPerformance): DriverDailyPerformance = withContext(Dispatchers.IO) {
        val entity = DriverDailyPerformanceEntity.fromDomain(performance)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findByCompanyDriverAndDate(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate
    ): DriverDailyPerformance? = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndDriverIdAndMetricDate(companyId.value, driverId.value, date)?.toDomain()
    }

    override suspend fun findByCompanyAndDateRange(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DriverDailyPerformance> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndMetricDateBetween(companyId.value, startDate, endDate)
            .map { it.toDomain() }
    }

    override suspend fun findByDriverAndDateRange(
        companyId: CompanyId,
        driverId: DriverId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DriverDailyPerformance> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndDriverIdAndMetricDateBetween(
            companyId.value,
            driverId.value,
            startDate,
            endDate
        ).map { it.toDomain() }
    }

    @Transactional
    override suspend fun deleteByCompanyAndDate(companyId: CompanyId, date: LocalDate) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByCompanyIdAndMetricDate(companyId.value, date)
        }
    }
}