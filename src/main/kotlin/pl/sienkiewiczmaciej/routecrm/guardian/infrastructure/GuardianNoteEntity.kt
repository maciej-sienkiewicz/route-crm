package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNote
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteCategory
import pl.sienkiewiczmaciej.routecrm.guardian.note.GuardianNoteId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant

@Entity
@Table(
    name = "guardian_notes",
    indexes = [
        Index(name = "idx_guardian_notes_company", columnList = "company_id"),
        Index(name = "idx_guardian_notes_guardian", columnList = "company_id, guardian_id"),
        Index(name = "idx_guardian_notes_created", columnList = "guardian_id, created_at")
    ]
)
class GuardianNoteEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "guardian_id", nullable = false, length = 50)
    val guardianId: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val category: GuardianNoteCategory,

    @Column(columnDefinition = "text", nullable = false)
    val content: String,

    @Column(name = "created_by", nullable = false, length = 50)
    val createdBy: String,

    @Column(name = "created_by_name", nullable = false, length = 255)
    val createdByName: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = GuardianNote(
        id = GuardianNoteId(id),
        guardianId = GuardianId(guardianId),
        companyId = CompanyId(companyId),
        category = category,
        content = content,
        createdBy = UserId(createdBy),
        createdByName = createdByName,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(note: GuardianNote) = GuardianNoteEntity(
            id = note.id.value,
            guardianId = note.guardianId.value,
            companyId = note.companyId.value,
            category = note.category,
            content = note.content,
            createdBy = note.createdBy.value,
            createdByName = note.createdByName,
            createdAt = note.createdAt
        )
    }
}