// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/infrastructure/ActivityLogJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.activity.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityCategory

interface ActivityLogJpaRepository : JpaRepository<ActivityLogEntity, String> {

    fun findByCompanyIdAndAggregateIdOrderByTimestampDesc(
        companyId: String,
        aggregateId: String,
        pageable: Pageable
    ): Page<ActivityLogEntity>

    fun findByCompanyIdAndAggregateIdAndCategoryOrderByTimestampDesc(
        companyId: String,
        aggregateId: String,
        category: ActivityCategory,
        pageable: Pageable
    ): Page<ActivityLogEntity>

    fun findByCompanyIdAndCategoryOrderByTimestampDesc(
        companyId: String,
        category: ActivityCategory,
        pageable: Pageable
    ): Page<ActivityLogEntity>
}