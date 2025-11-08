// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/infrastructure/OptimizationTaskJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationStatus
import java.time.LocalDate

interface OptimizationTaskJpaRepository : JpaRepository<OptimizationTaskEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): OptimizationTaskEntity?

    fun findByCompanyIdAndDate(companyId: String, date: LocalDate): List<OptimizationTaskEntity>

    fun findByStatus(status: OptimizationStatus): List<OptimizationTaskEntity>
}