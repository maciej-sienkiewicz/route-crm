// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/infrastructure/DriverNoteJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface DriverNoteJpaRepository : JpaRepository<DriverNoteEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): DriverNoteEntity?

    fun findByCompanyIdAndDriverIdOrderByCreatedAtDesc(
        companyId: String,
        driverId: String
    ): List<DriverNoteEntity>
}