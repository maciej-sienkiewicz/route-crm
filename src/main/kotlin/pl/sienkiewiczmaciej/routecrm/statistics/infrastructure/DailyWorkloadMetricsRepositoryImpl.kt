package pl.sienkiewiczmaciej.routecrm.statistics.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyWorkloadMetrics
import pl.sienkiewiczmaciej.routecrm.statistics.domain.DailyWorkloadMetricsRepository
import java.time.LocalDate

@Repository
class DailyWorkloadMetricsRepositoryImpl(
    private val jpaRepository: DailyWorkloadMetricsJpaRepository
) : DailyWorkloadMetricsRepository {

    override suspend fun save(metrics: DailyWorkloadMetrics): DailyWorkloadMetrics = withContext(Dispatchers.IO) {
        val entity = DailyWorkloadMetricsEntity.fromDomain(metrics)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findByCompanyAndDate(companyId: CompanyId, date: LocalDate): DailyWorkloadMetrics? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndMetricDate(companyId.value, date)?.toDomain()
        }

    override suspend fun findByCompanyAndDateRange(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DailyWorkloadMetrics> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndMetricDateBetween(companyId.value, startDate, endDate)
            .map { it.toDomain() }
    }

    @Transactional
    override suspend fun deleteByCompanyAndDate(companyId: CompanyId, date: LocalDate) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByCompanyIdAndMetricDate(companyId.value, date)
        }
    }
}