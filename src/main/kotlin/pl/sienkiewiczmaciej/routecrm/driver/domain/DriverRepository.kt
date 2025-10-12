package pl.sienkiewiczmaciej.routecrm.driver.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface DriverRepository {
    suspend fun save(driver: Driver): Driver
    suspend fun findById(companyId: CompanyId, id: DriverId): Driver?
    suspend fun findAll(companyId: CompanyId, status: DriverStatus?, search: String?, pageable: Pageable): Page<Driver>
    suspend fun existsByEmail(companyId: CompanyId, email: String): Boolean
    suspend fun existsByEmailExcludingId(companyId: CompanyId, email: String, excludeId: DriverId): Boolean
    suspend fun existsByLicenseNumber(companyId: CompanyId, licenseNumber: String): Boolean
    suspend fun existsByLicenseNumberExcludingId(companyId: CompanyId, licenseNumber: String, excludeId: DriverId): Boolean
}