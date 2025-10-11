package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "guardian_assignments",
    indexes = [
        Index(name = "idx_assignments_company", columnList = "company_id"),
        Index(name = "idx_assignments_guardian", columnList = "company_id, guardian_id"),
        Index(name = "idx_assignments_child", columnList = "company_id, child_id")
    ]
)
class GuardianAssignmentEntity(
    @Id
    @Column(length = 50)
    val id: String = "GA-${UUID.randomUUID()}",

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "guardian_id", nullable = false, length = 50)
    val guardianId: String,

    @Column(name = "child_id", nullable = false, length = 50)
    val childId: String,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val relationship: GuardianRelationship,

    @Column(name = "is_primary", nullable = false)
    val isPrimary: Boolean = false,

    @Column(name = "can_pickup", nullable = false)
    val canPickup: Boolean = true,

    @Column(name = "can_authorize", nullable = false)
    val canAuthorize: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class GuardianRelationship {
    PARENT,
    LEGAL_GUARDIAN,
    GRANDPARENT,
    RELATIVE,
    OTHER
}