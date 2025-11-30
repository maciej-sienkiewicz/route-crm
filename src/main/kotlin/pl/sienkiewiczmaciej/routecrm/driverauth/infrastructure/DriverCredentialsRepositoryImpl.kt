package pl.sienkiewiczmaciej.routecrm.driverauth.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentials
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsId
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class DriverCredentialsRepositoryImpl(
    private val jpaRepository: DriverCredentialsJpaRepository
) : DriverCredentialsRepository {

    override suspend fun save(credentials: DriverCredentials): DriverCredentials =
        withContext(Dispatchers.IO) {
            val entity = DriverCredentialsEntity.fromDomain(credentials)
            jpaRepository.save(entity).toDomain()
        }

    override suspend fun findById(id: DriverCredentialsId): DriverCredentials? =
        withContext(Dispatchers.IO) {
            jpaRepository.findById(id.value).orElse(null)?.toDomain()
        }

    override suspend fun findByDriverId(
        companyId: CompanyId,
        driverId: DriverId
    ): DriverCredentials? = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndDriverId(
            companyId.value,
            driverId.value
        )?.toDomain()
    }

    override suspend fun findByPhoneNumber(
        companyId: CompanyId,
        phoneNumber: String
    ): DriverCredentials? = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndPhoneNumber(
            companyId.value,
            phoneNumber
        )?.toDomain()
    }

    override suspend fun existsByPhoneNumber(
        companyId: CompanyId,
        phoneNumber: String
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndPhoneNumber(
            companyId.value,
            phoneNumber
        )
    }

    override suspend fun delete(id: DriverCredentialsId) = withContext(Dispatchers.IO) {
        jpaRepository.deleteById(id.value)
    }
}