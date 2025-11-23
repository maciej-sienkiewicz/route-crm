package pl.sienkiewiczmaciej.routecrm.guardian

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.guardian.create.CreateGuardianCommand
import pl.sienkiewiczmaciej.routecrm.guardian.create.CreateGuardianResult
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianDetail
import pl.sienkiewiczmaciej.routecrm.guardian.list.GuardianListItem
import pl.sienkiewiczmaciej.routecrm.guardian.update.UpdateGuardianCommand
import pl.sienkiewiczmaciej.routecrm.guardian.update.UpdateGuardianResult
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

data class AddressRequest(
    @field:NotBlank(message = "Street is required")
    @field:Size(max = 255)
    val street: String,

    @field:NotBlank(message = "House number is required")
    @field:Size(max = 20)
    val houseNumber: String,

    @field:Size(max = 20)
    val apartmentNumber: String?,

    @field:NotBlank(message = "Postal code is required")
    @field:Pattern(regexp = "\\d{2}-\\d{3}", message = "Invalid postal code format (XX-XXX)")
    val postalCode: String,

    @field:NotBlank(message = "City is required")
    @field:Size(max = 100)
    val city: String
) {
    fun toDomain() = Address(
        street = street.trim(),
        houseNumber = houseNumber.trim(),
        apartmentNumber = apartmentNumber?.trim(),
        postalCode = postalCode.trim(),
        city = city.trim()
    )
}

data class AddressResponse(
    val street: String,
    val houseNumber: String,
    val apartmentNumber: String?,
    val postalCode: String,
    val city: String
) {
    companion object {
        fun from(address: Address) = AddressResponse(
            street = address.street,
            houseNumber = address.houseNumber,
            apartmentNumber = address.apartmentNumber,
            postalCode = address.postalCode,
            city = address.city
        )
    }
}

data class CreateGuardianRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 255)
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 255)
    val lastName: String,

    @field:Email(message = "Invalid email format")
    @field:Size(max = 255)
    val email: String?,

    @field:NotBlank(message = "Phone is required")
    val phone: String,

    @field:Valid
    val address: AddressRequest?,
) {
    fun toCommand(companyId: CompanyId) = CreateGuardianCommand(
        companyId = companyId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        address = address?.toDomain(),
    )
}

data class GuardianResponse(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String,
    val address: AddressResponse?,
    val childrenCount: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(result: CreateGuardianResult) = GuardianResponse(
            id = result.id.value,
            companyId = result.companyId.value,
            firstName = result.firstName,
            lastName = result.lastName,
            email = result.email,
            phone = result.phone,
            address = result.address?.let { AddressResponse.from(it) } ,
            childrenCount = 0,
            createdAt = Instant.now()
        )
    }
}

data class GuardianListResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String,
    val childrenCount: Int
) {
    companion object {
        fun from(item: GuardianListItem) = GuardianListResponse(
            id = item.id.value,
            firstName = item.firstName,
            lastName = item.lastName,
            email = item.email,
            phone = item.phone,
            childrenCount = item.childrenCount
        )
    }
}

data class GuardianDetailResponse(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String,
    val address: AddressResponse?,
    val children: List<ChildInfoResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(detail: GuardianDetail) = GuardianDetailResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
            firstName = detail.firstName,
            lastName = detail.lastName,
            email = detail.email,
            phone = detail.phone,
            address = detail.address?.let { AddressResponse.from(it) } ,
            children = detail.children.map { ChildInfoResponse.from(it) },
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

data class ChildInfoResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val relationship: String,
    val isPrimary: Boolean
) {
    companion object {
        fun from(info: pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianChildInfo) = ChildInfoResponse(
            id = info.id,
            firstName = info.firstName,
            lastName = info.lastName,
            age = info.age,
            relationship = info.relationship,
            isPrimary = info.isPrimary
        )
    }
}

data class UpdateGuardianRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 255)
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 255)
    val lastName: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank(message = "Phone is required")
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    val phone: String,

    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    val alternatePhone: String?,

    @field:Valid
    val address: AddressRequest,
    ) {
    fun toCommand(companyId: CompanyId, id: GuardianId) = UpdateGuardianCommand(
        companyId = companyId,
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        alternatePhone = alternatePhone,
        address = address.toDomain(),
    )
}

data class UpdateGuardianResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateGuardianResult) = UpdateGuardianResponse(
            id = result.id.value,
            firstName = result.firstName,
            lastName = result.lastName,
            email = result.email,
            updatedAt = Instant.now()
        )
    }
}