// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/infrastructure/ActivityLogRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.activity.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityCategory
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityLog
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityLogRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class ActivityLogRepositoryImpl(
    private val jpaRepository: ActivityLogJpaRepository
) : ActivityLogRepository {

    override suspend fun save(activityLog: ActivityLog): ActivityLog = withContext(Dispatchers.IO) {
        val entity = ActivityLogEntity.fromDomain(activityLog)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findByAggregate(
        companyId: CompanyId,
        aggregateId: String,
        pageable: Pageable
    ): Page<ActivityLog> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndAggregateIdOrderByTimestampDesc(
            companyId.value,
            aggregateId,
            pageable
        ).map { it.toDomain() }
    }

    override suspend fun findByAggregateAndCategory(
        companyId: CompanyId,
        aggregateId: String,
        category: ActivityCategory,
        pageable: Pageable
    ): Page<ActivityLog> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndAggregateIdAndCategoryOrderByTimestampDesc(
            companyId.value,
            aggregateId,
            category,
            pageable
        ).map { it.toDomain() }
    }

    override suspend fun findByCategory(
        companyId: CompanyId,
        category: ActivityCategory,
        pageable: Pageable
    ): Page<ActivityLog> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndCategoryOrderByTimestampDesc(
            companyId.value,
            category,
            pageable
        ).map { it.toDomain() }
    }
}