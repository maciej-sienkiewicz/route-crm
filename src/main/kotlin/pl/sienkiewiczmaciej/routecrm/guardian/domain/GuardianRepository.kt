package pl.sienkiewiczmaciej.routecrm.guardian.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface GuardianRepository {
    suspend fun save(guardian: Guardian): Guardian
    suspend fun findById(companyId: CompanyId, id: GuardianId): Guardian?
    suspend fun findAll(companyId: CompanyId, search: String?, pageable: Pageable): Page<Guardian>
    suspend fun existsByEmail(companyId: CompanyId, email: String): Boolean
    suspend fun existsByEmailExcludingId(companyId: CompanyId, email: String, excludeId: GuardianId): Boolean
}