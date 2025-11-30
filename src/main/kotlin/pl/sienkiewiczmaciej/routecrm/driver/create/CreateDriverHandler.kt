package pl.sienkiewiczmaciej.routecrm.driver.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.*
import pl.sienkiewiczmaciej.routecrm.driverauth.service.DriverAuthService
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class CreateDriverCommand(
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val dateOfBirth: LocalDate,
    val address: Address,
    val drivingLicense: DrivingLicense,
    val medicalCertificate: MedicalCertificate
)

data class CreateDriverResult(
    val id: DriverId,
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val status: DriverStatus,
    val activationPin: String,
    val pinExpiresAt: Instant
)

@Component
class CreateDriverHandler(
    private val driverRepository: DriverRepository,
    private val driverAuthService: DriverAuthService,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateDriverCommand): CreateDriverResult {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        if (driverRepository.existsByEmail(command.companyId, command.email)) {
            throw IllegalArgumentException("Driver with email ${command.email} already exists")
        }

        if (driverRepository.existsByLicenseNumber(command.companyId, command.drivingLicense.licenseNumber)) {
            throw IllegalArgumentException("Driver with license number ${command.drivingLicense.licenseNumber} already exists")
        }

        val driver = Driver.create(
            companyId = command.companyId,
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            dateOfBirth = command.dateOfBirth,
            address = command.address,
            drivingLicense = command.drivingLicense,
            medicalCertificate = command.medicalCertificate
        )

        val saved = driverRepository.save(driver)

        val activation = driverAuthService.createCredentialsForNewDriver(
            companyId = saved.companyId,
            driverId = saved.id,
            phoneNumber = saved.phone
        )

        return CreateDriverResult(
            id = saved.id,
            companyId = saved.companyId,
            firstName = saved.firstName,
            lastName = saved.lastName,
            email = saved.email,
            status = saved.status,
            activationPin = activation.activationPin,
            pinExpiresAt = activation.expiresAt
        )
    }
}