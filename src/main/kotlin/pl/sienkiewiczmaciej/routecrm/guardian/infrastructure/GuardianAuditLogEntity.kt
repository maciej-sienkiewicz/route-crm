package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "guardian_audit_log",
    indexes = [
        Index(name = "idx_guardian_audit_global", columnList = "global_guardian_id, created_at"),
        Index(name = "idx_guardian_audit_company", columnList = "company_id, created_at"),
        Index(name = "idx_guardian_audit_type", columnList = "event_type, created_at")
    ]
)
class GuardianAuditLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "global_guardian_id", nullable = false, length = 50)
    val globalGuardianId: String,

    @Column(name = "company_id", length = 50)
    val companyId: String?,

    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: String,

    @Column(name = "event_data", columnDefinition = "jsonb")
    val eventData: String? = null,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "text")
    val userAgent: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)