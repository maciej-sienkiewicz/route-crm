package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.guardian.domain.CommunicationPreference
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

@Entity
@Table(
    name = "guardians",
    indexes = [
        Index(name = "idx_guardians_company", columnList = "company_id"),
        Index(name = "idx_guardians_company_email", columnList = "company_id, email", unique = true),
        Index(name = "idx_guardians_company_name", columnList = "company_id, last_name, first_name")
    ]
)
class GuardianEntity(
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

    @Column(name = "alternate_phone", length = 20)
    val alternatePhone: String?,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "street", column = Column(name = "address_street", nullable = false)),
        AttributeOverride(name = "houseNumber", column = Column(name = "address_house_number", nullable = false)),
        AttributeOverride(name = "apartmentNumber", column = Column(name = "address_apartment_number")),
        AttributeOverride(name = "postalCode", column = Column(name = "address_postal_code", nullable = false)),
        AttributeOverride(name = "city", column = Column(name = "address_city", nullable = false))
    )
    val address: AddressEmbeddable,

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_preference", nullable = false, length = 20)
    val communicationPreference: CommunicationPreference,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = Guardian(
        id = GuardianId(id),
        companyId = CompanyId(companyId),
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        alternatePhone = alternatePhone,
        address = address.toDomain(),
        communicationPreference = communicationPreference
    )

    companion object {
        fun fromDomain(guardian: Guardian) = GuardianEntity(
            id = guardian.id.value,
            companyId = guardian.companyId.value,
            firstName = guardian.firstName,
            lastName = guardian.lastName,
            email = guardian.email,
            phone = guardian.phone,
            alternatePhone = guardian.alternatePhone,
            address = AddressEmbeddable.fromDomain(guardian.address),
            communicationPreference = guardian.communicationPreference
        )
    }
}

@Embeddable
data class AddressEmbeddable(
    val street: String,
    val houseNumber: String,
    val apartmentNumber: String?,
    val postalCode: String,
    val city: String
) {
    fun toDomain() = Address(
        street = street,
        houseNumber = houseNumber,
        apartmentNumber = apartmentNumber,
        postalCode = postalCode,
        city = city
    )

    companion object {
        fun fromDomain(address: Address) = AddressEmbeddable(
            street = address.street,
            houseNumber = address.houseNumber,
            apartmentNumber = address.apartmentNumber,
            postalCode = address.postalCode,
            city = address.city
        )
    }
}