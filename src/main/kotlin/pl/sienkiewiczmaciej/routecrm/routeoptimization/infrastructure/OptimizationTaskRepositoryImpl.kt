// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/infrastructure/OptimizationTaskRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationStatus
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationTask
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationTaskId
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationTaskRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Repository
class OptimizationTaskRepositoryImpl(
    private val jpaRepository: OptimizationTaskJpaRepository
) : OptimizationTaskRepository {

    override suspend fun save(task: OptimizationTask): OptimizationTask = withContext(Dispatchers.IO) {
        val entity = OptimizationTaskEntity.fromDomain(task)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: OptimizationTaskId): OptimizationTask? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByCompanyAndDate(companyId: CompanyId, date: LocalDate): List<OptimizationTask> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndDate(companyId.value, date)
                .map { it.toDomain() }
        }

    override suspend fun findPendingTasks(): List<OptimizationTask> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByStatus(OptimizationStatus.PENDING)
                .map { it.toDomain() }
        }
}