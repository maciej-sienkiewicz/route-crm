package pl.sienkiewiczmaciej.routecrm.guardian.contact

import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant
import java.util.*

@JvmInline
value class ContactHistoryId(val value: String) {
    companion object {
        fun generate() = ContactHistoryId("CH-${UUID.randomUUID()}")
        fun from(value: String) = ContactHistoryId(value)
    }
}

enum class ContactType {
    PHONE_CALL,
    EMAIL,
    IN_PERSON,
    OTHER
}

data class ContactHistory(
    val id: ContactHistoryId,
    val guardianId: GuardianId,
    val companyId: CompanyId,
    val type: ContactType,
    val subject: String,
    val notes: String,
    val contactedAt: Instant,
    val handledBy: UserId,
    val handledByName: String
) {
    companion object {
        fun create(
            guardianId: GuardianId,
            companyId: CompanyId,
            type: ContactType,
            subject: String,
            notes: String,
            handledBy: UserId,
            handledByName: String
        ): ContactHistory {
            require(subject.isNotBlank()) { "Contact subject is required" }
            require(subject.length <= 500) { "Subject cannot exceed 500 characters" }
            require(notes.length <= 5000) { "Notes cannot exceed 5000 characters" }

            return ContactHistory(
                id = ContactHistoryId.generate(),
                guardianId = guardianId,
                companyId = companyId,
                type = type,
                subject = subject.trim(),
                notes = notes.trim(),
                contactedAt = Instant.now(),
                handledBy = handledBy,
                handledByName = handledByName
            )
        }
    }
}

interface ContactHistoryRepository {
    suspend fun save(contact: ContactHistory): ContactHistory
    suspend fun findById(companyId: CompanyId, id: ContactHistoryId): ContactHistory?
    suspend fun findByGuardian(companyId: CompanyId, guardianId: GuardianId): List<ContactHistory>
    suspend fun delete(companyId: CompanyId, id: ContactHistoryId)
}
