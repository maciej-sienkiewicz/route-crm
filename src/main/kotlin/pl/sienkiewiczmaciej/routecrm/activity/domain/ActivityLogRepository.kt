// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/domain/ActivityLogRepository.kt
package pl.sienkiewiczmaciej.routecrm.activity.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface ActivityLogRepository {
    suspend fun save(activityLog: ActivityLog): ActivityLog

    suspend fun findByAggregate(
        companyId: CompanyId,
        aggregateId: String,
        pageable: Pageable
    ): Page<ActivityLog>

    suspend fun findByAggregateAndCategory(
        companyId: CompanyId,
        aggregateId: String,
        category: ActivityCategory,
        pageable: Pageable
    ): Page<ActivityLog>

    suspend fun findByCategory(
        companyId: CompanyId,
        category: ActivityCategory,
        pageable: Pageable
    ): Page<ActivityLog>
}