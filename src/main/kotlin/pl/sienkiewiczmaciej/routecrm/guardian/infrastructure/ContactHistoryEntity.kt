package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistory
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryId
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactType
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant

@Entity
@Table(
    name = "guardian_contact_history",
    indexes = [
        Index(name = "idx_contact_history_company", columnList = "company_id"),
        Index(name = "idx_contact_history_guardian", columnList = "company_id, guardian_id"),
        Index(name = "idx_contact_history_contacted", columnList = "guardian_id, contacted_at")
    ]
)
class ContactHistoryEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "guardian_id", nullable = false, length = 50)
    val guardianId: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: ContactType,

    @Column(nullable = false, length = 500)
    val subject: String,

    @Column(columnDefinition = "text", nullable = false)
    val notes: String,

    @Column(name = "contacted_at", nullable = false)
    val contactedAt: Instant,

    @Column(name = "handled_by", nullable = false, length = 50)
    val handledBy: String,

    @Column(name = "handled_by_name", nullable = false, length = 255)
    val handledByName: String
) {
    fun toDomain() = ContactHistory(
        id = ContactHistoryId(id),
        guardianId = GuardianId(guardianId),
        companyId = CompanyId(companyId),
        type = type,
        subject = subject,
        notes = notes,
        contactedAt = contactedAt,
        handledBy = UserId(handledBy),
        handledByName = handledByName
    )

    companion object {
        fun fromDomain(contact: ContactHistory) = ContactHistoryEntity(
            id = contact.id.value,
            guardianId = contact.guardianId.value,
            companyId = contact.companyId.value,
            type = contact.type,
            subject = contact.subject,
            notes = contact.notes,
            contactedAt = contact.contactedAt,
            handledBy = contact.handledBy.value,
            handledByName = contact.handledByName
        )
    }
}