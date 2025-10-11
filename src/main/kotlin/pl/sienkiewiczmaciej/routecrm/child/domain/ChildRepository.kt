package pl.sienkiewiczmaciej.routecrm.child.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface ChildRepository {
    suspend fun save(child: Child): Child
    suspend fun findById(companyId: CompanyId, id: ChildId): Child?
    suspend fun findAll(companyId: CompanyId, status: ChildStatus?, pageable: Pageable): Page<Child>
    suspend fun findByGuardian(companyId: CompanyId, guardianId: GuardianId, pageable: Pageable): Page<Child>
    suspend fun delete(companyId: CompanyId, id: ChildId)
}