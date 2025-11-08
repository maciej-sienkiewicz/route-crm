// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/infrastructure/OptimizationTaskEntity.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationStatus
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationTask
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationTaskId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "optimization_tasks",
    indexes = [
        Index(name = "idx_optimization_tasks_company", columnList = "company_id"),
        Index(name = "idx_optimization_tasks_company_date", columnList = "company_id, date"),
        Index(name = "idx_optimization_tasks_status", columnList = "status"),
        Index(name = "idx_optimization_tasks_created", columnList = "created_at")
    ]
)
class OptimizationTaskEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(nullable = false)
    val date: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: OptimizationStatus,

    @Column(name = "request_data", columnDefinition = "text", nullable = false)
    val requestData: String,

    @Column(name = "response_data", columnDefinition = "text")
    val responseData: String?,

    @Column(name = "error_message", columnDefinition = "text")
    val errorMessage: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "completed_at")
    val completedAt: Instant?
) {
    fun toDomain() = OptimizationTask(
        id = OptimizationTaskId(id),
        companyId = CompanyId(companyId),
        date = date,
        status = status,
        requestData = requestData,
        responseData = responseData,
        errorMessage = errorMessage,
        createdAt = createdAt,
        completedAt = completedAt
    )

    companion object {
        fun fromDomain(task: OptimizationTask) = OptimizationTaskEntity(
            id = task.id.value,
            companyId = task.companyId.value,
            date = task.date,
            status = task.status,
            requestData = task.requestData,
            responseData = task.responseData,
            errorMessage = task.errorMessage,
            createdAt = task.createdAt,
            completedAt = task.completedAt
        )
    }
}