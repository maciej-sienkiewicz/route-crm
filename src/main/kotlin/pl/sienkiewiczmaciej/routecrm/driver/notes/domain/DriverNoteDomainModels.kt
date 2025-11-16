// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/domain/DriverNoteDomainModels.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant
import java.util.*

@JvmInline
value class DriverNoteId(val value: String) {
    companion object {
        fun generate() = DriverNoteId("DNOTE-${UUID.randomUUID()}")
        fun from(value: String) = DriverNoteId(value)
    }
}

enum class DriverNoteCategory {
    REMINDER,
    PRAISE,
    ISSUE,
    OTHER
}

data class DriverNote(
    val id: DriverNoteId,
    val companyId: CompanyId,
    val driverId: DriverId,
    val category: DriverNoteCategory,
    val content: String,
    val createdBy: UserId,
    val createdByName: String,
    val createdAt: Instant
) {
    companion object {
        fun create(
            companyId: CompanyId,
            driverId: DriverId,
            category: DriverNoteCategory,
            content: String,
            createdBy: UserId,
            createdByName: String
        ): DriverNote {
            require(content.isNotBlank()) { "Note content is required" }
            require(content.length <= 5000) { "Note content cannot exceed 5000 characters" }

            return DriverNote(
                id = DriverNoteId.generate(),
                companyId = companyId,
                driverId = driverId,
                category = category,
                content = content.trim(),
                createdBy = createdBy,
                createdByName = createdByName,
                createdAt = Instant.now()
            )
        }
    }

    fun update(category: DriverNoteCategory, content: String): DriverNote {
        require(content.isNotBlank()) { "Note content is required" }
        require(content.length <= 5000) { "Note content cannot exceed 5000 characters" }

        return copy(
            category = category,
            content = content.trim()
        )
    }
}