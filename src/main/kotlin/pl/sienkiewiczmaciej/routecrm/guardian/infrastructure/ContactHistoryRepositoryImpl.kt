package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistory
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryId
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class ContactHistoryRepositoryImpl(
    private val jpaRepository: ContactHistoryJpaRepository
) : ContactHistoryRepository {

    override suspend fun save(contact: ContactHistory): ContactHistory = withContext(Dispatchers.IO) {
        val entity = ContactHistoryEntity.fromDomain(contact)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: ContactHistoryId): ContactHistory? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByGuardian(companyId: CompanyId, guardianId: GuardianId): List<ContactHistory> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByGuardian(companyId.value, guardianId.value).map { it.toDomain() }
        }

    override suspend fun delete(companyId: CompanyId, id: ContactHistoryId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}