package pl.sienkiewiczmaciej.routecrm.user.infrastructure

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "companies")
class CompanyEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)