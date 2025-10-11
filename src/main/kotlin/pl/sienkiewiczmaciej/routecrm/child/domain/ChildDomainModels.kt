package pl.sienkiewiczmaciej.routecrm.child.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate
import java.time.Period
import java.util.*

@JvmInline
value class ChildId(val value: String) {
    companion object {
        fun generate() = ChildId("CH-${UUID.randomUUID()}")
        fun from(value: String) = ChildId(value)
    }
}

enum class ChildStatus {
    ACTIVE,
    INACTIVE,
    TEMPORARY_INACTIVE
}

enum class DisabilityType {
    INTELLECTUAL,
    PHYSICAL,
    SENSORY_VISUAL,
    SENSORY_HEARING,
    AUTISM,
    MULTIPLE,
    SPEECH,
    MENTAL
}

data class TransportNeeds(
    val wheelchair: Boolean,
    val specialSeat: Boolean,
    val safetyBelt: Boolean
)

data class Child(
    val id: ChildId,
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeeds,
    val notes: String?,
    val status: ChildStatus
) {
    fun age(): Int = Period.between(birthDate, LocalDate.now()).years

    companion object {
        fun create(
            companyId: CompanyId,
            firstName: String,
            lastName: String,
            birthDate: LocalDate,
            disability: Set<DisabilityType>,
            transportNeeds: TransportNeeds,
            notes: String?
        ): Child {
            require(firstName.isNotBlank()) { "First name is required" }
            require(lastName.isNotBlank()) { "Last name is required" }
            require(birthDate.isBefore(LocalDate.now())) { "Birth date must be in the past" }
            require(disability.isNotEmpty()) { "At least one disability type is required" }

            return Child(
                id = ChildId.generate(),
                companyId = companyId,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                birthDate = birthDate,
                disability = disability,
                transportNeeds = transportNeeds,
                notes = notes?.trim(),
                status = ChildStatus.ACTIVE
            )
        }
    }

    fun update(
        firstName: String,
        lastName: String,
        birthDate: LocalDate,
        status: ChildStatus,
        disability: Set<DisabilityType>,
        transportNeeds: TransportNeeds,
        notes: String?
    ): Child {
        require(firstName.isNotBlank()) { "First name is required" }
        require(lastName.isNotBlank()) { "Last name is required" }
        require(birthDate.isBefore(LocalDate.now())) { "Birth date must be in the past" }
        require(disability.isNotEmpty()) { "At least one disability type is required" }

        return copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            birthDate = birthDate,
            status = status,
            disability = disability,
            transportNeeds = transportNeeds,
            notes = notes?.trim()
        )
    }
}