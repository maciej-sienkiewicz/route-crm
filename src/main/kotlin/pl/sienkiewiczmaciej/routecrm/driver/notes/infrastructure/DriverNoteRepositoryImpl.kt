// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/infrastructure/DriverNoteRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNote
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class DriverNoteRepositoryImpl(
    private val jpaRepository: DriverNoteJpaRepository
) : DriverNoteRepository {

    override suspend fun save(note: DriverNote): DriverNote = withContext(Dispatchers.IO) {
        val entity = DriverNoteEntity.fromDomain(note)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: DriverNoteId): DriverNote? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByDriver(companyId: CompanyId, driverId: DriverId): List<DriverNote> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndDriverIdOrderByCreatedAtDesc(
                companyId.value,
                driverId.value
            ).map { it.toDomain() }
        }

    override suspend fun delete(companyId: CompanyId, id: DriverNoteId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}