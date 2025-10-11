package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GuardianJpaRepository : JpaRepository<GuardianEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): GuardianEntity?

    fun findByCompanyId(companyId: String, pageable: Pageable): Page<GuardianEntity>

    @Query("""
        SELECT g FROM GuardianEntity g 
        WHERE g.companyId = :companyId 
        AND (
            LOWER(g.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(g.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(g.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR g.phone LIKE CONCAT('%', :search, '%')
        )
    """)
    fun findByCompanyIdAndSearch(
        companyId: String,
        search: String,
        pageable: Pageable
    ): Page<GuardianEntity>

    fun existsByCompanyIdAndEmail(companyId: String, email: String): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END 
        FROM GuardianEntity g 
        WHERE g.companyId = :companyId 
        AND g.email = :email 
        AND g.id != :excludeId
    """)
    fun existsByCompanyIdAndEmailExcludingId(
        companyId: String,
        email: String,
        excludeId: String
    ): Boolean
}