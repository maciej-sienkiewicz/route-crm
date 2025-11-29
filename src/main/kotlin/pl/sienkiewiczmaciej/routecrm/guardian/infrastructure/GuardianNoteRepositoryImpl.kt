// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/guardian/infrastructure/GuardianNoteRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNote
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class GuardianNoteRepositoryImpl(
    private val jpaRepository: GuardianNoteJpaRepository
) : GuardianNoteRepository {

    override suspend fun save(note: GuardianNote): GuardianNote = withContext(Dispatchers.IO) {
        val entity = GuardianNoteEntity.fromDomain(note)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: GuardianNoteId): GuardianNote? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByGuardian(companyId: CompanyId, guardianId: GuardianId): List<GuardianNote> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByGuardian(companyId.value, guardianId.value).map { it.toDomain() }
        }

    override suspend fun delete(companyId: CompanyId, id: GuardianNoteId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}