package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class GuardianRepositoryImpl(
    private val jpaRepository: GuardianJpaRepository
) : GuardianRepository {

    override suspend fun save(guardian: Guardian): Guardian = withContext(Dispatchers.IO) {
        val entity = GuardianEntity.fromDomain(guardian)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: GuardianId): Guardian? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findAll(
        companyId: CompanyId,
        search: String?,
        pageable: Pageable
    ): Page<Guardian> = withContext(Dispatchers.IO) {
        val page = if (search != null) {
            jpaRepository.findByCompanyIdAndSearch(companyId.value, search, pageable)
        } else {
            jpaRepository.findByCompanyId(companyId.value, pageable)
        }
        page.map { it.toDomain() }
    }

    override suspend fun existsByEmail(companyId: CompanyId, email: String): Boolean =
        withContext(Dispatchers.IO) {
            jpaRepository.existsByCompanyIdAndEmail(companyId.value, email)
        }

    override suspend fun existsByEmailExcludingId(
        companyId: CompanyId,
        email: String,
        excludeId: GuardianId
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndEmailExcludingId(companyId.value, email, excludeId.value)
    }
}