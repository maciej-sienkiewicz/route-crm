// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/domain/ActivityDomainModels.kt
package pl.sienkiewiczmaciej.routecrm.activity.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant
import java.util.*

@JvmInline
value class ActivityLogId(val value: String) {
    companion object {
        fun generate() = ActivityLogId("ACT-${UUID.randomUUID()}")
        fun from(value: String) = ActivityLogId(value)
    }
}

enum class ActivityCategory {
    CHILD,
    GUARDIAN,
    ROUTE,
    DRIVER,
    VEHICLE,
    SCHEDULE,
    SYSTEM
}

enum class ActivityType {
    // Child activities
    CHILD_CREATED,
    CHILD_UPDATED,
    CHILD_STATUS_CHANGED,
    CHILD_ASSIGNED_TO_ROUTE,
    CHILD_REMOVED_FROM_ROUTE,
    CHILD_DELETED,

    // Guardian activities
    GUARDIAN_CREATED,
    GUARDIAN_UPDATED,
    GUARDIAN_DELETED,
    GUARDIAN_CHILD_ADDED,
    GUARDIAN_LOGGED_IN,
    GUARDIAN_PASSWORD_CHANGED,

    // Route activities
    ROUTE_CREATED,
    ROUTE_STATUS_CHANGED,
    ROUTE_SCHEDULE_ADDED,
    ROUTE_SCHEDULE_CANCELLED,
    ROUTE_SCHEDULE_DELETED,
    ROUTE_STOP_EXECUTED,
    ROUTE_STOP_UPDATED,
    ROUTE_STOPS_REORDERED,
    ROUTE_NOTE_ADDED,
    ROUTE_DELETED,

    // Schedule activities
    SCHEDULE_CREATED,
    SCHEDULE_UPDATED,
    SCHEDULE_DELETED,

    // Driver activities
    DRIVER_CREATED,
    DRIVER_UPDATED,
    DRIVER_STATUS_CHANGED,
    DRIVER_ASSIGNED_TO_ROUTE,
    DRIVER_DELETED,

    // Vehicle activities
    VEHICLE_CREATED,
    VEHICLE_UPDATED,
    VEHICLE_STATUS_CHANGED,
    VEHICLE_ASSIGNED_TO_ROUTE,
    VEHICLE_DELETED
}

data class ActivityMetadata(
    val data: Map<String, Any?>
) {
    companion object {
        fun empty() = ActivityMetadata(emptyMap())

        fun of(vararg pairs: Pair<String, Any?>) = ActivityMetadata(
            pairs.toMap().filterValues { it != null }
        )
    }

    fun with(key: String, value: Any?): ActivityMetadata {
        return if (value != null) {
            copy(data = data + (key to value))
        } else {
            this
        }
    }
}

data class ActivityDetails(
    val data: Map<String, Any?>
) {
    companion object {
        fun empty() = ActivityDetails(emptyMap())

        fun of(vararg pairs: Pair<String, Any?>) = ActivityDetails(
            pairs.toMap().filterValues { it != null }
        )
    }

    fun with(key: String, value: Any?): ActivityDetails {
        return if (value != null) {
            copy(data = data + (key to value))
        } else {
            this
        }
    }
}

data class ActivityPerformer(
    val userId: UserId?,
    val userName: String,
    val userRole: String?
) {
    companion object {
        fun system() = ActivityPerformer(
            userId = null,
            userName = "System",
            userRole = "SYSTEM"
        )

        fun fromPrincipal(
            userId: UserId,
            firstName: String,
            lastName: String,
            role: String
        ) = ActivityPerformer(
            userId = userId,
            userName = "$firstName $lastName",
            userRole = role
        )
    }
}

data class ActivityLog(
    val id: ActivityLogId,
    val companyId: CompanyId,
    val category: ActivityCategory,
    val activityType: ActivityType,
    val aggregateId: String,
    val aggregateType: String,
    val title: String,
    val description: String,
    val details: ActivityDetails,
    val performedBy: ActivityPerformer,
    val metadata: ActivityMetadata,
    val timestamp: Instant,
    val eventId: String?
) {
    companion object {
        fun create(
            companyId: CompanyId,
            category: ActivityCategory,
            activityType: ActivityType,
            aggregateId: String,
            aggregateType: String,
            title: String,
            description: String,
            details: ActivityDetails = ActivityDetails.empty(),
            performedBy: ActivityPerformer,
            metadata: ActivityMetadata = ActivityMetadata.empty(),
            eventId: String? = null
        ): ActivityLog {
            require(title.isNotBlank()) { "Activity title is required" }
            require(description.isNotBlank()) { "Activity description is required" }
            require(aggregateId.isNotBlank()) { "Aggregate ID is required" }
            require(aggregateType.isNotBlank()) { "Aggregate type is required" }

            return ActivityLog(
                id = ActivityLogId.generate(),
                companyId = companyId,
                category = category,
                activityType = activityType,
                aggregateId = aggregateId,
                aggregateType = aggregateType,
                title = title.trim(),
                description = description.trim(),
                details = details,
                performedBy = performedBy,
                metadata = metadata,
                timestamp = Instant.now(),
                eventId = eventId
            )
        }
    }
}