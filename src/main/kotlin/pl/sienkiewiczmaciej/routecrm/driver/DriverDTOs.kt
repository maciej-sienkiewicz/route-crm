package pl.sienkiewiczmaciej.routecrm.driver

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import pl.sienkiewiczmaciej.routecrm.driver.create.CreateDriverCommand
import pl.sienkiewiczmaciej.routecrm.driver.create.CreateDriverResult
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.domain.DrivingLicense
import pl.sienkiewiczmaciej.routecrm.driver.domain.MedicalCertificate
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverAuthStatus
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverDetail
import pl.sienkiewiczmaciej.routecrm.driver.list.DriverListItem
import pl.sienkiewiczmaciej.routecrm.driver.update.UpdateDriverCommand
import pl.sienkiewiczmaciej.routecrm.driver.update.UpdateDriverResult
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.AccountStatus
import pl.sienkiewiczmaciej.routecrm.guardian.AddressRequest
import pl.sienkiewiczmaciej.routecrm.guardian.AddressResponse
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate

data class DrivingLicenseRequest(
    @field:NotBlank(message = "License number is required")
    @field:Size(max = 50)
    val licenseNumber: String,

    @field:NotEmpty(message = "At least one category is required")
    val categories: Set<String>,

    @field:NotNull(message = "License valid until date is required")
    @field:Future(message = "License must be valid")
    val validUntil: LocalDate
) {
    fun toDomain() = DrivingLicense(
        licenseNumber = licenseNumber.trim(),
        categories = categories,
        validUntil = validUntil
    )
}

data class DrivingLicenseResponse(
    val licenseNumber: String,
    val categories: Set<String>,
    val validUntil: LocalDate
) {
    companion object {
        fun from(license: DrivingLicense) = DrivingLicenseResponse(
            licenseNumber = license.licenseNumber,
            categories = license.categories,
            validUntil = license.validUntil
        )
    }
}

data class MedicalCertificateRequest(
    @field:NotNull(message = "Medical certificate valid until date is required")
    @field:Future(message = "Medical certificate must be valid")
    val validUntil: LocalDate,

    @field:NotNull(message = "Issue date is required")
    @field:PastOrPresent(message = "Issue date cannot be in the future")
    val issueDate: LocalDate
) {
    fun toDomain() = MedicalCertificate(
        validUntil = validUntil,
        issueDate = issueDate
    )
}

data class MedicalCertificateResponse(
    val validUntil: LocalDate,
    val issueDate: LocalDate
) {
    companion object {
        fun from(certificate: MedicalCertificate) = MedicalCertificateResponse(
            validUntil = certificate.validUntil,
            issueDate = certificate.issueDate
        )
    }
}

data class CreateDriverRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 255)
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 255)
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank(message = "Phone is required")
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    val phone: String,

    @field:NotNull(message = "Date of birth is required")
    @field:Past(message = "Date of birth must be in the past")
    val dateOfBirth: LocalDate,

    @field:Valid
    @field:NotNull(message = "Address is required")
    val address: AddressRequest,

    @field:Valid
    @field:NotNull(message = "Driving license is required")
    val drivingLicense: DrivingLicenseRequest,

    @field:Valid
    @field:NotNull(message = "Medical certificate is required")
    val medicalCertificate: MedicalCertificateRequest
) {
    fun toCommand(companyId: CompanyId) = CreateDriverCommand(
        companyId = companyId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        dateOfBirth = dateOfBirth,
        address = address.toDomain(),
        drivingLicense = drivingLicense.toDomain(),
        medicalCertificate = medicalCertificate.toDomain()
    )
}

data class DriverResponse(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val dateOfBirth: LocalDate,
    val address: AddressResponse,
    val drivingLicense: DrivingLicenseResponse,
    val medicalCertificate: MedicalCertificateResponse,
    val status: DriverStatus,
    val createdAt: Instant,
    val activationPin: String,
    val pinExpiresAt: Instant
) {
    companion object {
        fun from(result: CreateDriverResult, command: CreateDriverCommand) = DriverResponse(
            id = result.id.value,
            companyId = result.companyId.value,
            firstName = result.firstName,
            lastName = result.lastName,
            email = result.email,
            phone = command.phone,
            dateOfBirth = command.dateOfBirth,
            address = AddressResponse.from(command.address),
            drivingLicense = DrivingLicenseResponse.from(command.drivingLicense),
            medicalCertificate = MedicalCertificateResponse.from(command.medicalCertificate),
            status = result.status,
            createdAt = Instant.now(),
            activationPin = result.activationPin,
            pinExpiresAt = result.pinExpiresAt
        )
    }
}

data class DriverListResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
    val status: DriverStatus,
    val drivingLicense: DrivingLicenseSimpleResponse,
    val medicalCertificate: MedicalCertificateSimpleResponse,
    val todayRoutesCount: Int
) {
    companion object {
        fun from(item: DriverListItem) = DriverListResponse(
            id = item.id.value,
            firstName = item.firstName,
            lastName = item.lastName,
            phone = item.phone,
            email = item.email,
            status = item.status,
            drivingLicense = DrivingLicenseSimpleResponse(
                categories = item.licenseCategories,
                validUntil = item.licenseValidUntil
            ),
            medicalCertificate = MedicalCertificateSimpleResponse(
                validUntil = item.medicalCertificateValidUntil
            ),
            todayRoutesCount = item.todayRoutesCount
        )
    }
}

data class DrivingLicenseSimpleResponse(
    val categories: Set<String>,
    val validUntil: LocalDate
)

data class MedicalCertificateSimpleResponse(
    val validUntil: LocalDate
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DriverAuthStatusResponse(
    val accountStatus: AccountStatus,
    val accountStatusDisplay: String,
    val failedLoginAttempts: Int,
    val lastFailedLoginAt: Instant?,
    val lockedAt: Instant?,
    val lockedUntil: Instant?,
    val isCurrentlyLocked: Boolean,
    val activatedAt: Instant?,
    val passwordChangedAt: Instant,
    val hasCredentials: Boolean,
    val canLogin: Boolean,
    val requiresActivation: Boolean
) {
    companion object {
        fun from(authStatus: DriverAuthStatus): DriverAuthStatusResponse {
            val isCurrentlyLocked = authStatus.lockedUntil?.isAfter(Instant.now()) ?: false
            val requiresActivation = authStatus.accountStatus == AccountStatus.PENDING_ACTIVATION
            val canLogin = authStatus.accountStatus == AccountStatus.ACTIVE && !isCurrentlyLocked

            return DriverAuthStatusResponse(
                accountStatus = authStatus.accountStatus,
                accountStatusDisplay = when (authStatus.accountStatus) {
                    AccountStatus.PENDING_ACTIVATION -> "Oczekuje na aktywację"
                    AccountStatus.ACTIVE -> if (isCurrentlyLocked) "Zablokowane" else "Aktywne"
                    AccountStatus.LOCKED -> "Zablokowane"
                    AccountStatus.SUSPENDED -> "Zawieszone"
                },
                failedLoginAttempts = authStatus.failedLoginAttempts,
                lastFailedLoginAt = authStatus.lastFailedLoginAt,
                lockedAt = authStatus.lockedAt,
                lockedUntil = authStatus.lockedUntil,
                isCurrentlyLocked = isCurrentlyLocked,
                activatedAt = authStatus.activatedAt,
                passwordChangedAt = authStatus.passwordChangedAt,
                hasCredentials = authStatus.hasCredentials,
                canLogin = canLogin,
                requiresActivation = requiresActivation
            )
        }
    }
}

data class DriverDetailResponse(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val dateOfBirth: LocalDate,
    val address: AddressResponse,
    val drivingLicense: DrivingLicenseResponse,
    val medicalCertificate: MedicalCertificateResponse,
    val status: DriverStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val authStatus: DriverAuthStatusResponse?
) {
    companion object {
        fun from(detail: DriverDetail) = DriverDetailResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
            firstName = detail.firstName,
            lastName = detail.lastName,
            email = detail.email,
            phone = detail.phone,
            dateOfBirth = detail.dateOfBirth,
            address = AddressResponse.from(detail.address),
            drivingLicense = DrivingLicenseResponse.from(detail.drivingLicense),
            medicalCertificate = MedicalCertificateResponse.from(detail.medicalCertificate),
            status = detail.status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            authStatus = detail.authStatus?.let { DriverAuthStatusResponse.from(it) }
        )
    }
}

data class UpdateDriverRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 255)
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 255)
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank(message = "Phone is required")
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    val phone: String,

    @field:Valid
    @field:NotNull(message = "Address is required")
    val address: AddressRequest,

    @field:NotNull(message = "Status is required")
    val status: DriverStatus,

    @field:Valid
    @field:NotNull(message = "Driving license is required")
    val drivingLicense: DrivingLicenseRequest,

    @field:Valid
    @field:NotNull(message = "Medical certificate is required")
    val medicalCertificate: MedicalCertificateRequest
) {
    fun toCommand(companyId: CompanyId, id: DriverId) = UpdateDriverCommand(
        companyId = companyId,
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        address = address.toDomain(),
        status = status,
        drivingLicense = drivingLicense.toDomain(),
        medicalCertificate = medicalCertificate.toDomain()
    )
}

data class UpdateDriverResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val status: DriverStatus,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateDriverResult) = UpdateDriverResponse(
            id = result.id.value,
            firstName = result.firstName,
            lastName = result.lastName,
            status = result.status,
            updatedAt = Instant.now()
        )
    }
}