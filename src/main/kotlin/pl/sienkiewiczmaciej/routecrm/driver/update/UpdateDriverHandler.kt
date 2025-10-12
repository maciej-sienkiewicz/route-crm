package pl.sienkiewiczmaciej.routecrm.driver.update

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.*
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class UpdateDriverCommand(
    val companyId: CompanyId,
    val id: DriverId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: Address,
    val status: DriverStatus,
    val drivingLicense: DrivingLicense,
    val medicalCertificate: MedicalCertificate
)

data class UpdateDriverResult(
    val id: DriverId,
    val firstName: String,
    val lastName: String,
    val status: DriverStatus
)

@Component
class UpdateDriverHandler(
    private val driverRepository: DriverRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateDriverCommand): UpdateDriverResult {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val driver = driverRepository.findById(command.companyId, command.id)
            ?: throw DriverNotFoundException(command.id)

        if (driverRepository.existsByEmailExcludingId(command.companyId, command.email, command.id)) {
            throw IllegalArgumentException("Driver with email ${command.email} already exists")
        }

        if (driverRepository.existsByLicenseNumberExcludingId(
                command.companyId,
                command.drivingLicense.licenseNumber,
                command.id
            )
        ) {
            throw IllegalArgumentException("Driver with license number ${command.drivingLicense.licenseNumber} already exists")
        }

        val updated = driver.update(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            address = command.address,
            status = command.status,
            drivingLicense = command.drivingLicense,
            medicalCertificate = command.medicalCertificate
        )

        val saved = driverRepository.save(updated)

        return UpdateDriverResult(
            id = saved.id,
            firstName = saved.firstName,
            lastName = saved.lastName,
            status = saved.status
        )
    }
}