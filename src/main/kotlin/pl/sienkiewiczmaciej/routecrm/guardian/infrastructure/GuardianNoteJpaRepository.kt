package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GuardianNoteJpaRepository : JpaRepository<GuardianNoteEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): GuardianNoteEntity?

    @Query("""
        SELECT n FROM GuardianNoteEntity n
        WHERE n.companyId = :companyId
        AND n.guardianId = :guardianId
        ORDER BY n.createdAt DESC
    """)
    fun findByGuardian(companyId: String, guardianId: String): List<GuardianNoteEntity>
}