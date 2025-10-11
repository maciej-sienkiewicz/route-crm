package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GuardianAssignmentJpaRepository : JpaRepository<GuardianAssignmentEntity, String> {

    fun findByCompanyIdAndGuardianIdAndChildId(
        companyId: String,
        guardianId: String,
        childId: String
    ): GuardianAssignmentEntity?

    fun findByCompanyIdAndChildId(companyId: String, childId: String): List<GuardianAssignmentEntity>

    fun findByCompanyIdAndGuardianId(companyId: String, guardianId: String): List<GuardianAssignmentEntity>

    fun deleteByCompanyIdAndGuardianIdAndChildId(
        companyId: String,
        guardianId: String,
        childId: String
    )

    @Query("""
        SELECT COUNT(ga) FROM GuardianAssignmentEntity ga 
        WHERE ga.companyId = :companyId AND ga.guardianId = :guardianId
    """)
    fun countByCompanyIdAndGuardianId(companyId: String, guardianId: String): Long

    @Query("""
        SELECT COUNT(ga) FROM GuardianAssignmentEntity ga 
        WHERE ga.companyId = :companyId AND ga.childId = :childId
    """)
    fun countByCompanyIdAndChildId(companyId: String, childId: String): Long
}