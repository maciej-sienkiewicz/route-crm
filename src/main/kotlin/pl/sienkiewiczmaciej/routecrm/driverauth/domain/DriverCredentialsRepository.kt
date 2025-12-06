package pl.sienkiewiczmaciej.routecrm.driverauth.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface DriverCredentialsRepository {
    suspend fun save(credentials: DriverCredentials): DriverCredentials

    suspend fun findById(id: DriverCredentialsId): DriverCredentials?

    suspend fun findByDriverId(companyId: CompanyId, driverId: DriverId): DriverCredentials?

    suspend fun findByPhoneNumber(phoneNumber: String): DriverCredentials?

    suspend fun existsByPhoneNumber(companyId: CompanyId, phoneNumber: String): Boolean

    suspend fun delete(id: DriverCredentialsId)
}