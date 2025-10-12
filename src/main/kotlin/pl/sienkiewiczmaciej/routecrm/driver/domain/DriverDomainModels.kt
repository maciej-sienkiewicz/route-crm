package pl.sienkiewiczmaciej.routecrm.driver.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate
import java.time.Period
import java.util.*

@JvmInline
value class DriverId(val value: String) {
    companion object {
        fun generate() = DriverId("DRV-${UUID.randomUUID()}")
        fun from(value: String) = DriverId(value)
    }
}

enum class DriverStatus {
    ACTIVE,
    INACTIVE,
    ON_LEAVE
}

data class DrivingLicense(
    val licenseNumber: String,
    val categories: Set<String>,
    val validUntil: LocalDate
) {
    init {
        require(licenseNumber.isNotBlank()) { "License number is required" }
        require(categories.isNotEmpty()) { "At least one category is required" }
        require(categories.contains("D")) { "Category D is required for bus drivers" }
        require(validUntil.isAfter(LocalDate.now())) { "License must be valid" }
    }
}

data class MedicalCertificate(
    val validUntil: LocalDate,
    val issueDate: LocalDate
) {
    init {
        require(validUntil.isAfter(LocalDate.now())) { "Medical certificate must be valid" }
        require(issueDate.isBefore(validUntil)) { "Issue date must be before expiry date" }
    }
}

data class Driver(
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
    val status: DriverStatus
) {
    fun age(): Int = Period.between(dateOfBirth, LocalDate.now()).years

    companion object {
        fun create(
            companyId: CompanyId,
            firstName: String,
            lastName: String,
            email: String,
            phone: String,
            dateOfBirth: LocalDate,
            address: Address,
            drivingLicense: DrivingLicense,
            medicalCertificate: MedicalCertificate
        ): Driver {
            require(firstName.isNotBlank()) { "First name is required" }
            require(lastName.isNotBlank()) { "Last name is required" }
            require(email.isNotBlank()) { "Email is required" }
            require(email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                "Invalid email format"
            }
            require(phone.isNotBlank()) { "Phone is required" }
            require(phone.matches(Regex("^\\+?[1-9]\\d{1,14}$"))) {
                "Invalid phone format"
            }

            val age = Period.between(dateOfBirth, LocalDate.now()).years
            require(age >= 21) { "Driver must be at least 21 years old" }

            return Driver(
                id = DriverId.generate(),
                companyId = companyId,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                email = email.trim().lowercase(),
                phone = phone.trim(),
                dateOfBirth = dateOfBirth,
                address = address,
                drivingLicense = drivingLicense,
                medicalCertificate = medicalCertificate,
                status = DriverStatus.ACTIVE
            )
        }
    }

    fun update(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        address: Address,
        status: DriverStatus,
        drivingLicense: DrivingLicense,
        medicalCertificate: MedicalCertificate
    ): Driver {
        require(firstName.isNotBlank()) { "First name is required" }
        require(lastName.isNotBlank()) { "Last name is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            "Invalid email format"
        }
        require(phone.isNotBlank()) { "Phone is required" }

        return copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim().lowercase(),
            phone = phone.trim(),
            address = address,
            status = status,
            drivingLicense = drivingLicense,
            medicalCertificate = medicalCertificate
        )
    }
}