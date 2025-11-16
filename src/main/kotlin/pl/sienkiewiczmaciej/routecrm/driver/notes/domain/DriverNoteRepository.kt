// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/domain/DriverNoteRepository.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface DriverNoteRepository {
    suspend fun save(note: DriverNote): DriverNote
    suspend fun findById(companyId: CompanyId, id: DriverNoteId): DriverNote?
    suspend fun findByDriver(companyId: CompanyId, driverId: DriverId): List<DriverNote>
    suspend fun delete(companyId: CompanyId, id: DriverNoteId)
}