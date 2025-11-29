package pl.sienkiewiczmaciej.routecrm.guardian.note

import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant
import java.util.*

@JvmInline
value class GuardianNoteId(val value: String) {
    companion object {
        fun generate() = GuardianNoteId("GN-${UUID.randomUUID()}")
        fun from(value: String) = GuardianNoteId(value)
    }
}

enum class GuardianNoteCategory {
    GENERAL,
    COMPLAINT,
    PRAISE,
    PAYMENT,
    URGENT
}

data class GuardianNote(
    val id: GuardianNoteId,
    val guardianId: GuardianId,
    val companyId: CompanyId,
    val category: GuardianNoteCategory,
    val content: String,
    val createdBy: UserId,
    val createdByName: String,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            guardianId: GuardianId,
            companyId: CompanyId,
            category: GuardianNoteCategory,
            content: String,
            createdBy: UserId,
            createdByName: String
        ): GuardianNote {
            require(content.isNotBlank()) { "Note content is required" }
            require(content.length <= 5000) { "Note content cannot exceed 5000 characters" }

            return GuardianNote(
                id = GuardianNoteId.generate(),
                guardianId = guardianId,
                companyId = companyId,
                category = category,
                content = content.trim(),
                createdBy = createdBy,
                createdByName = createdByName
            )
        }
    }

    fun update(category: GuardianNoteCategory, content: String): GuardianNote {
        require(content.isNotBlank()) { "Note content is required" }
        require(content.length <= 5000) { "Note content cannot exceed 5000 characters" }

        return copy(
            category = category,
            content = content.trim()
        )
    }
}

interface GuardianNoteRepository {
    suspend fun save(note: GuardianNote): GuardianNote
    suspend fun findById(companyId: CompanyId, id: GuardianNoteId): GuardianNote?
    suspend fun findByGuardian(companyId: CompanyId, guardianId: GuardianId): List<GuardianNote>
    suspend fun delete(companyId: CompanyId, id: GuardianNoteId)
}