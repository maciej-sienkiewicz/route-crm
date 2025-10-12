package pl.sienkiewiczmaciej.routecrm.driver.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class DriverRepositoryImpl(
    private val jpaRepository: DriverJpaRepository
) : DriverRepository {

    override suspend fun save(driver: Driver): Driver = withContext(Dispatchers.IO) {
        val entity = DriverEntity.fromDomain(driver)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: DriverId): Driver? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findAll(
        companyId: CompanyId,
        status: DriverStatus?,
        search: String?,
        pageable: Pageable
    ): Page<Driver> = withContext(Dispatchers.IO) {
        val page = when {
            status != null && search != null -> {
                jpaRepository.findByCompanyIdAndStatusAndSearch(
                    companyId.value,
                    status,
                    search,
                    pageable
                )
            }
            status != null -> {
                jpaRepository.findByCompanyIdAndStatus(companyId.value, status, pageable)
            }
            search != null -> {
                jpaRepository.findByCompanyIdAndSearch(companyId.value, search, pageable)
            }
            else -> {
                jpaRepository.findByCompanyId(companyId.value, pageable)
            }
        }
        page.map { it.toDomain() }
    }

    override suspend fun existsByEmail(companyId: CompanyId, email: String): Boolean =
        withContext(Dispatchers.IO) {
            jpaRepository.existsByCompanyIdAndEmail(companyId.value, email)
        }

    override suspend fun existsByEmailExcludingId(
        companyId: CompanyId,
        email: String,
        excludeId: DriverId
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndEmailExcludingId(companyId.value, email, excludeId.value)
    }

    override suspend fun existsByLicenseNumber(companyId: CompanyId, licenseNumber: String): Boolean =
        withContext(Dispatchers.IO) {
            jpaRepository.existsByCompanyIdAndLicenseNumber(companyId.value, licenseNumber)
        }

    override suspend fun existsByLicenseNumberExcludingId(
        companyId: CompanyId,
        licenseNumber: String,
        excludeId: DriverId
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndLicenseNumberExcludingId(
            companyId.value,
            licenseNumber,
            excludeId.value
        )
    }
}