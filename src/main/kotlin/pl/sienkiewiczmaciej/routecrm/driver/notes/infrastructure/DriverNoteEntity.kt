// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/infrastructure/DriverNoteEntity.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNote
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteCategory
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant

@Entity
@Table(
    name = "driver_notes",
    indexes = [
        Index(name = "idx_driver_notes_company", columnList = "company_id"),
        Index(name = "idx_driver_notes_driver", columnList = "company_id, driver_id"),
        Index(name = "idx_driver_notes_created", columnList = "driver_id, created_at")
    ]
)
class DriverNoteEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "driver_id", nullable = false, length = 50)
    val driverId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val category: DriverNoteCategory,

    @Column(columnDefinition = "text", nullable = false)
    val content: String,

    @Column(name = "created_by", nullable = false, length = 50)
    val createdBy: String,

    @Column(name = "created_by_name", nullable = false, length = 255)
    val createdByName: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain() = DriverNote(
        id = DriverNoteId(id),
        companyId = CompanyId(companyId),
        driverId = DriverId(driverId),
        category = category,
        content = content,
        createdBy = UserId(createdBy),
        createdByName = createdByName,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(note: DriverNote) = DriverNoteEntity(
            id = note.id.value,
            companyId = note.companyId.value,
            driverId = note.driverId.value,
            category = note.category,
            content = note.content,
            createdBy = note.createdBy.value,
            createdByName = note.createdByName,
            createdAt = note.createdAt
        )
    }
}