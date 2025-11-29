package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.auth.global.AccountStatus
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianId
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
        Index(name = "idx_guardians_company_email", columnList = "company_id, email"),
        Index(name = "idx_guardians_company_name", columnList = "company_id, last_name, first_name"),
        Index(name = "idx_guardians_global_id", columnList = "company_id, global_guardian_id"),
        Index(name = "idx_guardians_account_status", columnList = "company_id, account_status"),
        Index(name = "idx_guardians_last_login", columnList = "company_id, last_login")
    ]
)
class GuardianEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "global_guardian_id", length = 50)
    val globalGuardianId: String?,

    @Column(name = "first_name", nullable = false, length = 255)
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 255)
    val lastName: String,

    @Column(nullable = true, length = 255)
    val email: String?,

    @Column(nullable = false, length = 20)
    val phone: String,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "street", column = Column(name = "address_street", nullable = true)),
        AttributeOverride(name = "houseNumber", column = Column(name = "address_house_number", nullable = true)),
        AttributeOverride(name = "apartmentNumber", column = Column(name = "address_apartment_number")),
        AttributeOverride(name = "postalCode", column = Column(name = "address_postal_code", nullable = true)),
        AttributeOverride(name = "address_city", column = Column(name = "address_city", nullable = true))
    )
    val address: AddressEmbeddable?,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,

    @Column(name = "last_login")
    val lastLogin: Instant? = null,

    @Column(name = "login_count", nullable = false)
    val loginCount: Int = 0,

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
        address = address?.toDomain()
    )

    fun withGlobalGuardianId(globalId: GlobalGuardianId): GuardianEntity {
        return GuardianEntity(
            id = id,
            companyId = companyId,
            globalGuardianId = globalId.value,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            address = address,
            accountStatus = accountStatus,
            lastLogin = lastLogin,
            loginCount = loginCount,
            createdAt = createdAt,
            updatedAt = Instant.now()
        )
    }

    fun recordLogin(): GuardianEntity {
        return GuardianEntity(
            id = id,
            companyId = companyId,
            globalGuardianId = globalGuardianId,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            address = address,
            accountStatus = accountStatus,
            lastLogin = Instant.now(),
            loginCount = loginCount + 1,
            createdAt = createdAt,
            updatedAt = Instant.now()
        )
    }

    companion object {
        fun fromDomain(guardian: Guardian, globalGuardianId: GlobalGuardianId? = null) = GuardianEntity(
            id = guardian.id.value,
            companyId = guardian.companyId.value,
            globalGuardianId = globalGuardianId?.value,
            firstName = guardian.firstName,
            lastName = guardian.lastName,
            email = guardian.email,
            phone = guardian.phone,
            address = guardian.address?.let { AddressEmbeddable.fromDomain(it) },
            accountStatus = AccountStatus.ACTIVE,
            lastLogin = null,
            loginCount = 0
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