package pl.sienkiewiczmaciej.routecrm.driverauth.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface DriverCredentialsJpaRepository : JpaRepository<DriverCredentialsEntity, String> {

    fun findByCompanyIdAndDriverId(companyId: String, driverId: String): DriverCredentialsEntity?

    fun findByCompanyIdAndPhoneNumber(companyId: String, phoneNumber: String): DriverCredentialsEntity?

    fun existsByCompanyIdAndPhoneNumber(companyId: String, phoneNumber: String): Boolean
}