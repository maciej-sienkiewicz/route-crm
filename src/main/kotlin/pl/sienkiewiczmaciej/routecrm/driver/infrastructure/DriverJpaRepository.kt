package pl.sienkiewiczmaciej.routecrm.driver.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus

interface DriverJpaRepository : JpaRepository<DriverEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): DriverEntity?

    fun findByCompanyId(companyId: String, pageable: Pageable): Page<DriverEntity>

    fun findByCompanyIdAndStatus(
        companyId: String,
        status: DriverStatus,
        pageable: Pageable
    ): Page<DriverEntity>

    @Query("""
        SELECT d FROM DriverEntity d
        WHERE d.companyId = :companyId
        AND (
            LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR d.licenseNumber LIKE CONCAT('%', :search, '%')
        )
    """)
    fun findByCompanyIdAndSearch(
        companyId: String,
        search: String,
        pageable: Pageable
    ): Page<DriverEntity>

    @Query("""
        SELECT d FROM DriverEntity d
        WHERE d.companyId = :companyId
        AND d.status = :status
        AND (
            LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR d.licenseNumber LIKE CONCAT('%', :search, '%')
        )
    """)
    fun findByCompanyIdAndStatusAndSearch(
        companyId: String,
        status: DriverStatus,
        search: String,
        pageable: Pageable
    ): Page<DriverEntity>

    fun existsByCompanyIdAndEmail(companyId: String, email: String): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
        FROM DriverEntity d
        WHERE d.companyId = :companyId
        AND d.email = :email
        AND d.id != :excludeId
    """)
    fun existsByCompanyIdAndEmailExcludingId(
        companyId: String,
        email: String,
        excludeId: String
    ): Boolean

    fun existsByCompanyIdAndLicenseNumber(companyId: String, licenseNumber: String): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
        FROM DriverEntity d
        WHERE d.companyId = :companyId
        AND d.licenseNumber = :licenseNumber
        AND d.id != :excludeId
    """)
    fun existsByCompanyIdAndLicenseNumberExcludingId(
        companyId: String,
        licenseNumber: String,
        excludeId: String
    ): Boolean
}