package pl.sienkiewiczmaciej.routecrm.child.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class ChildRepositoryImpl(
    private val jpaRepository: ChildJpaRepository
) : ChildRepository {

    override suspend fun save(child: Child): Child = withContext(Dispatchers.IO) {
        val entity = ChildEntity.fromDomain(child)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: ChildId): Child? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findAll(
        companyId: CompanyId,
        status: ChildStatus?,
        pageable: Pageable
    ): Page<Child> = withContext(Dispatchers.IO) {
        val page = if (status != null) {
            jpaRepository.findByCompanyIdAndStatus(companyId.value, status, pageable)
        } else {
            jpaRepository.findByCompanyId(companyId.value, pageable)
        }
        page.map { it.toDomain() }
    }

    override suspend fun findByGuardian(
        companyId: CompanyId,
        guardianId: GuardianId,
        pageable: Pageable
    ): Page<Child> = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndGuardianId(
            companyId.value,
            guardianId.value,
            pageable
        ).map { it.toDomain() }
    }

    override suspend fun delete(companyId: CompanyId, id: ChildId) {
        withContext(Dispatchers.IO) {
            jpaRepository.softDeleteByIdAndCompanyId(id.value, companyId.value)
        }
    }
}