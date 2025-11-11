// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/list/ListActivitiesHandler.kt
package pl.sienkiewiczmaciej.routecrm.activity.list

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityCategory
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityLogRepository
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityType
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class ListActivitiesQuery(
    val companyId: CompanyId,
    val aggregateId: String,
    val category: ActivityCategory?,
    val pageable: Pageable
)

data class ActivityListItem(
    val id: String,
    val category: ActivityCategory,
    val type: ActivityType,
    val title: String,
    val description: String,
    val details: Map<String, Any?>,
    val performedBy: String,
    val performedByRole: String?,
    val timestamp: Instant,
    val metadata: Map<String, Any?>
)

@Component
class ListActivitiesHandler(
    private val activityLogRepository: ActivityLogRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListActivitiesQuery): Page<ActivityListItem> {
        // Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        // Query activities
        val activities = if (query.category != null) {
            activityLogRepository.findByAggregateAndCategory(
                query.companyId,
                query.aggregateId,
                query.category,
                query.pageable
            )
        } else {
            activityLogRepository.findByAggregate(
                query.companyId,
                query.aggregateId,
                query.pageable
            )
        }

        // Map to list items
        return activities.map { activity ->
            ActivityListItem(
                id = activity.id.value,
                category = activity.category,
                type = activity.activityType,
                title = activity.title,
                description = activity.description,
                details = activity.details.data,
                performedBy = activity.performedBy.userName,
                performedByRole = activity.performedBy.userRole,
                timestamp = activity.timestamp,
                metadata = activity.metadata.data
            )
        }
    }
}