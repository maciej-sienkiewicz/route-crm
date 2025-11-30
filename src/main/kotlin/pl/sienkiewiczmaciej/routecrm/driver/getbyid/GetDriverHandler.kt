package pl.sienkiewiczmaciej.routecrm.driver.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.*
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.AccountStatus
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class GetDriverQuery(
    val companyId: CompanyId,
    val id: DriverId
)

data class DriverDetail(
    val id: DriverId,
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val dateOfBirth: LocalDate,
    val address: Address,
    val drivingLicense: DrivingLicense,
    val medicalCertificate: MedicalCertificate,
    val status: DriverStatus,
    val authStatus: DriverAuthStatus?
)

data class DriverAuthStatus(
    val accountStatus: AccountStatus,
    val failedLoginAttempts: Int,
    val lastFailedLoginAt: Instant?,
    val lockedAt: Instant?,
    val lockedUntil: Instant?,
    val activatedAt: Instant?,
    val passwordChangedAt: Instant,
    val hasCredentials: Boolean
)

class DriverNotFoundException(id: DriverId) : NotFoundException("Driver ${id.value} not found")

@Component
class GetDriverHandler(
    private val driverRepository: DriverRepository,
    private val credentialsRepository: DriverCredentialsRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDriverQuery): DriverDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val driver = driverRepository.findById(query.companyId, query.id)
            ?: throw DriverNotFoundException(query.id)

        val credentials = credentialsRepository.findByDriverId(query.companyId, query.id)

        val authStatus = credentials?.let {
            DriverAuthStatus(
                accountStatus = it.accountStatus,
                failedLoginAttempts = it.failedLoginAttempts,
                lastFailedLoginAt = it.lastFailedLoginAt,
                lockedAt = it.lockedAt,
                lockedUntil = it.lockedUntil,
                activatedAt = it.activatedAt,
                passwordChangedAt = it.passwordChangedAt,
                hasCredentials = true
            )
        }

        return DriverDetail(
            id = driver.id,
            companyId = driver.companyId,
            firstName = driver.firstName,
            lastName = driver.lastName,
            email = driver.email,
            phone = driver.phone,
            dateOfBirth = driver.dateOfBirth,
            address = driver.address,
            drivingLicense = driver.drivingLicense,
            medicalCertificate = driver.medicalCertificate,
            status = driver.status,
            authStatus = authStatus
        )
    }
}