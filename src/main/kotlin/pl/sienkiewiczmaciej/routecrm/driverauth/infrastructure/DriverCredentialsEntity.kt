package pl.sienkiewiczmaciej.routecrm.driverauth.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.AccountStatus
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentials
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

@Entity
@Table(
    name = "driver_credentials",
    indexes = [
        Index(name = "idx_driver_credentials_company", columnList = "company_id"),
        Index(name = "idx_driver_credentials_driver", columnList = "company_id, driver_id", unique = true),
        Index(name = "idx_driver_credentials_phone", columnList = "company_id, phone_number", unique = true),
        Index(name = "idx_driver_credentials_status", columnList = "company_id, account_status")
    ]
)
class DriverCredentialsEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "driver_id", nullable = false, length = 50)
    val driverId: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "phone_number", nullable = false, length = 20)
    val phoneNumber: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 50)
    val accountStatus: AccountStatus,

    @Column(name = "failed_login_attempts", nullable = false)
    val failedLoginAttempts: Int,

    @Column(name = "last_failed_login_at")
    val lastFailedLoginAt: Instant?,

    @Column(name = "locked_at")
    val lockedAt: Instant?,

    @Column(name = "locked_until")
    val lockedUntil: Instant?,

    @Column(name = "activation_pin", length = 10)
    val activationPin: String?,

    @Column(name = "activated_at")
    val activatedAt: Instant?,

    @Column(name = "password_changed_at", nullable = false)
    val passwordChangedAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant
) {
    fun toDomain() = DriverCredentials(
        id = DriverCredentialsId(id),
        driverId = DriverId(driverId),
        companyId = CompanyId(companyId),
        phoneNumber = phoneNumber,
        passwordHash = passwordHash,
        accountStatus = accountStatus,
        failedLoginAttempts = failedLoginAttempts,
        lastFailedLoginAt = lastFailedLoginAt,
        lockedAt = lockedAt,
        lockedUntil = lockedUntil,
        activationPin = activationPin,
        activatedAt = activatedAt,
        passwordChangedAt = passwordChangedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(credentials: DriverCredentials) = DriverCredentialsEntity(
            id = credentials.id.value,
            driverId = credentials.driverId.value,
            companyId = credentials.companyId.value,
            phoneNumber = credentials.phoneNumber,
            passwordHash = credentials.passwordHash,
            accountStatus = credentials.accountStatus,
            failedLoginAttempts = credentials.failedLoginAttempts,
            lastFailedLoginAt = credentials.lastFailedLoginAt,
            lockedAt = credentials.lockedAt,
            lockedUntil = credentials.lockedUntil,
            activationPin = credentials.activationPin,
            activatedAt = credentials.activatedAt,
            passwordChangedAt = credentials.passwordChangedAt,
            createdAt = credentials.createdAt,
            updatedAt = credentials.updatedAt
        )
    }
}