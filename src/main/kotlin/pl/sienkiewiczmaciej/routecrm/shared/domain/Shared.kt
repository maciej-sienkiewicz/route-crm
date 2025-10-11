package pl.sienkiewiczmaciej.routecrm.shared.domain

import java.io.Serializable
import java.util.*

@JvmInline
value class CompanyId(val value: String) : Serializable {
    companion object {
        fun generate() = CompanyId("CMP-${UUID.randomUUID()}")
        fun from(value: String) = CompanyId(value)
    }
}

@JvmInline
value class UserId(val value: String) : Serializable {
    companion object {
        fun generate() = UserId("USR-${UUID.randomUUID()}")
        fun from(value: String) = UserId(value)
    }
}

data class Address(
    val street: String,
    val houseNumber: String,
    val apartmentNumber: String? = null,
    val postalCode: String,
    val city: String
) : Serializable {
    init {
        require(street.isNotBlank()) { "Street is required" }
        require(houseNumber.isNotBlank()) { "House number is required" }
        require(postalCode.matches(Regex("\\d{2}-\\d{3}"))) { "Invalid postal code format" }
        require(city.isNotBlank()) { "City is required" }
    }
}

enum class UserRole {
    ADMIN,
    OPERATOR,
    GUARDIAN,
    DRIVER
}

data class UserPrincipal(
    val userId: UserId,
    val companyId: CompanyId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val guardianId: String? = null,
    val driverId: String? = null
) : Serializable