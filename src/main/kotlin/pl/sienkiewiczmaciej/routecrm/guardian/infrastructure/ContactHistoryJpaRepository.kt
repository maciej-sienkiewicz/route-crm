// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/guardian/infrastructure/ContactHistoryJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ContactHistoryJpaRepository : JpaRepository<ContactHistoryEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): ContactHistoryEntity?

    @Query("""
        SELECT c FROM ContactHistoryEntity c
        WHERE c.companyId = :companyId
        AND c.guardianId = :guardianId
        ORDER BY c.contactedAt DESC
    """)
    fun findByGuardian(companyId: String, guardianId: String): List<ContactHistoryEntity>
}