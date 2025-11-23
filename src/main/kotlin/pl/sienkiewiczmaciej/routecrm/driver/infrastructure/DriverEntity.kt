package pl.sienkiewiczmaciej.routecrm.driver.infrastructure

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import pl.sienkiewiczmaciej.routecrm.driver.domain.*
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.AddressEmbeddable
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "drivers",
    indexes = [
        Index(name = "idx_drivers_company", columnList = "company_id"),
        Index(name = "idx_drivers_company_status", columnList = "company_id, status"),
        Index(name = "idx_drivers_company_email", columnList = "company_id, email", unique = true),
        Index(name = "idx_drivers_company_license", columnList = "company_id, license_number", unique = true)
    ]
)
class DriverEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "first_name", nullable = false, length = 255)
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 255)
    val lastName: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(nullable = false, length = 20)
    val phone: String,

    @Column(name = "date_of_birth", nullable = false)
    val dateOfBirth: LocalDate,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "street", column = Column(name = "address_street", nullable = false)),
        AttributeOverride(name = "houseNumber", column = Column(name = "address_house_number", nullable = false)),
        AttributeOverride(name = "apartmentNumber", column = Column(name = "address_apartment_number")),
        AttributeOverride(name = "postalCode", column = Column(name = "address_postal_code", nullable = false)),
        AttributeOverride(name = "city", column = Column(name = "address_city", nullable = false))
    )
    val address: AddressEmbeddable,

    @Column(name = "license_number", nullable = false, length = 50)
    val licenseNumber: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "license_categories", columnDefinition = "jsonb", nullable = false)
    val licenseCategories: Set<String>,

    @Column(name = "license_valid_until", nullable = false)
    val licenseValidUntil: LocalDate,

    @Column(name = "medical_certificate_valid_until", nullable = false)
    val medicalCertificateValidUntil: LocalDate,

    @Column(name = "medical_certificate_issue_date", nullable = false)
    val medicalCertificateIssueDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: DriverStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = Driver(
        id = DriverId(id),
        companyId = CompanyId(companyId),
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        dateOfBirth = dateOfBirth,
        address = address.toDomain(),
        drivingLicense = DrivingLicense(
            licenseNumber = licenseNumber,
            categories = licenseCategories,
            validUntil = licenseValidUntil
        ),
        medicalCertificate = MedicalCertificate(
            validUntil = medicalCertificateValidUntil,
            issueDate = medicalCertificateIssueDate
        ),
        status = status
    )

    companion object {
        fun fromDomain(driver: Driver) = DriverEntity(
            id = driver.id.value,
            companyId = driver.companyId.value,
            firstName = driver.firstName,
            lastName = driver.lastName,
            email = driver.email,
            phone = driver.phone,
            dateOfBirth = driver.dateOfBirth,
            address = AddressEmbeddable.fromDomain(driver.address),
            licenseNumber = driver.drivingLicense.licenseNumber,
            licenseCategories = driver.drivingLicense.categories,
            licenseValidUntil = driver.drivingLicense.validUntil,
            medicalCertificateValidUntil = driver.medicalCertificate.validUntil,
            medicalCertificateIssueDate = driver.medicalCertificate.issueDate,
            status = driver.status
        )
    }
}