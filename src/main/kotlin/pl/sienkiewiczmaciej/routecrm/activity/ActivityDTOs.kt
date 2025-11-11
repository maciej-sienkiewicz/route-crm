// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/ActivityDTOs.kt
package pl.sienkiewiczmaciej.routecrm.activity

import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityCategory
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityType
import pl.sienkiewiczmaciej.routecrm.activity.list.ActivityListItem
import java.time.Instant

data class ActivityResponse(
    val id: String,
    val category: ActivityCategory,
    val type: ActivityType,
    val title: String,
    val description: String,
    val performedBy: PerformedByResponse,
    val timestamp: Instant,
    val metadata: Map<String, Any?>
) {
    companion object {
        fun from(item: ActivityListItem) = ActivityResponse(
            id = item.id,
            category = item.category,
            type = item.type,
            title = item.title,
            description = item.description,
            performedBy = PerformedByResponse(
                name = item.performedBy,
                role = item.performedByRole
            ),
            timestamp = item.timestamp,
            metadata = item.metadata
        )
    }
}

data class PerformedByResponse(
    val name: String,
    val role: String?
)