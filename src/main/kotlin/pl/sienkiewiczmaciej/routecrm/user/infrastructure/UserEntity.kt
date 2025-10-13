package pl.sienkiewiczmaciej.routecrm.user.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import java.time.Instant

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    val passwordHash: String,

    @Column(name = "first_name", nullable = false, length = 255)
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 255)
    val lastName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole,

    @Column(name = "guardian_id", length = 50)
    val guardianId: String?,

    @Column(name = "driver_id", length = 50)
    val driverId: String?,

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)