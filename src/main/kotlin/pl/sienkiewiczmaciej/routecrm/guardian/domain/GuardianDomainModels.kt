package pl.sienkiewiczmaciej.routecrm.guardian.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.util.*

@JvmInline
value class GuardianId(val value: String) {
    companion object {
        fun generate() = GuardianId("GRD-${UUID.randomUUID()}")
        fun from(value: String) = GuardianId(value)
    }
}

enum class CommunicationPreference {
    SMS,
    EMAIL,
    PHONE,
    APP
}

data class Guardian(
    val id: GuardianId,
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val alternatePhone: String?,
    val address: Address,
    val communicationPreference: CommunicationPreference
) {
    companion object {
        fun create(
            companyId: CompanyId,
            firstName: String,
            lastName: String,
            email: String,
            phone: String,
            alternatePhone: String?,
            address: Address,
            communicationPreference: CommunicationPreference
        ): Guardian {
            require(firstName.isNotBlank()) { "First name is required" }
            require(lastName.isNotBlank()) { "Last name is required" }
            require(email.isNotBlank()) { "Email is required" }
            require(email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                "Invalid email format"
            }
            require(phone.isNotBlank()) { "Phone is required" }
            require(phone.matches(Regex("^\\+?[1-9]\\d{1,14}$"))) {
                "Invalid phone format"
            }

            return Guardian(
                id = GuardianId.generate(),
                companyId = companyId,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                email = email.trim().lowercase(),
                phone = phone.trim(),
                alternatePhone = alternatePhone?.trim(),
                address = address,
                communicationPreference = communicationPreference
            )
        }
    }

    fun update(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        alternatePhone: String?,
        address: Address,
        communicationPreference: CommunicationPreference
    ): Guardian {
        require(firstName.isNotBlank()) { "First name is required" }
        require(lastName.isNotBlank()) { "Last name is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            "Invalid email format"
        }
        require(phone.isNotBlank()) { "Phone is required" }

        return copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim().lowercase(),
            phone = phone.trim(),
            alternatePhone = alternatePhone?.trim(),
            address = address,
            communicationPreference = communicationPreference
        )
    }
}