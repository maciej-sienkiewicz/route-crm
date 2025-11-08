// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/domain/OptimizationTaskRepository.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface OptimizationTaskRepository {
    suspend fun save(task: OptimizationTask): OptimizationTask
    suspend fun findById(companyId: CompanyId, id: OptimizationTaskId): OptimizationTask?
    suspend fun findByCompanyAndDate(companyId: CompanyId, date: LocalDate): List<OptimizationTask>
    suspend fun findPendingTasks(): List<OptimizationTask>
}