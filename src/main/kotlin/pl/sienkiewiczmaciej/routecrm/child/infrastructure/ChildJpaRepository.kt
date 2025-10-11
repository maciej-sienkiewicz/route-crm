package pl.sienkiewiczmaciej.routecrm.child.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus

interface ChildJpaRepository : JpaRepository<ChildEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): ChildEntity?

    fun findByCompanyId(companyId: String, pageable: Pageable): Page<ChildEntity>

    fun findByCompanyIdAndStatus(
        companyId: String,
        status: ChildStatus,
        pageable: Pageable
    ): Page<ChildEntity>

    @Query("""
        SELECT c FROM ChildEntity c 
        JOIN GuardianAssignmentEntity ga ON c.id = ga.childId 
        WHERE c.companyId = :companyId 
        AND ga.guardianId = :guardianId
    """)
    fun findByCompanyIdAndGuardianId(
        companyId: String,
        guardianId: String,
        pageable: Pageable
    ): Page<ChildEntity>

    @Modifying
    @Query("""
        UPDATE ChildEntity c 
        SET c.status = 'INACTIVE', c.updatedAt = CURRENT_TIMESTAMP 
        WHERE c.id = :id AND c.companyId = :companyId
    """)
    fun softDeleteByIdAndCompanyId(id: String, companyId: String): Int
}