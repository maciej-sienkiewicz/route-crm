// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/infrastructure/ActivityLogEntity.kt
package pl.sienkiewiczmaciej.routecrm.activity.infrastructure

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import pl.sienkiewiczmaciej.routecrm.activity.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant

@Entity
@Table(
    name = "activity_logs",
    indexes = [
        Index(name = "idx_activity_logs_company", columnList = "company_id"),
        Index(name = "idx_activity_logs_company_aggregate", columnList = "company_id, aggregate_id, timestamp"),
        Index(name = "idx_activity_logs_company_category", columnList = "company_id, category, timestamp"),
        Index(name = "idx_activity_logs_timestamp", columnList = "company_id, timestamp")
    ]
)
class ActivityLogEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val category: ActivityCategory,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 100)
    val activityType: ActivityType,

    @Column(name = "aggregate_id", nullable = false, length = 50)
    val aggregateId: String,

    @Column(name = "aggregate_type", nullable = false, length = 50)
    val aggregateType: String,

    @Column(nullable = false, length = 500)
    val title: String,

    @Column(nullable = false, columnDefinition = "text")
    val description: String,

    @Column(name = "performed_by_user_id", length = 50)
    val performedByUserId: String?,

    @Column(name = "performed_by_name", nullable = false, length = 255)
    val performedByName: String,

    @Column(name = "performed_by_role", length = 50)
    val performedByRole: String?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    val metadata: Map<String, Any?>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    val details: Map<String, Any?>,

    @Column(nullable = false)
    val timestamp: Instant,

    @Column(name = "event_id", length = 50)
    val eventId: String?
) {
    fun toDomain() = ActivityLog(
        id = ActivityLogId(id),
        companyId = CompanyId(companyId),
        category = category,
        activityType = activityType,
        aggregateId = aggregateId,
        aggregateType = aggregateType,
        title = title,
        description = description,
        performedBy = ActivityPerformer(
            userId = performedByUserId?.let { UserId(it) },
            userName = performedByName,
            userRole = performedByRole
        ),
        metadata = ActivityMetadata(metadata),
        timestamp = timestamp,
        eventId = eventId,
        details = ActivityDetails(details),
    )

    companion object {
        fun fromDomain(activity: ActivityLog) = ActivityLogEntity(
            id = activity.id.value,
            companyId = activity.companyId.value,
            category = activity.category,
            activityType = activity.activityType,
            aggregateId = activity.aggregateId,
            aggregateType = activity.aggregateType,
            title = activity.title,
            description = activity.description,
            performedByUserId = activity.performedBy.userId?.value,
            performedByName = activity.performedBy.userName,
            performedByRole = activity.performedBy.userRole,
            metadata = activity.metadata.data,
            timestamp = activity.timestamp,
            eventId = activity.eventId,
            details = activity.details.data
        )
    }
}