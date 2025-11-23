package pl.sienkiewiczmaciej.routecrm.child

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.child.create.CreateChildCommand
import pl.sienkiewiczmaciej.routecrm.child.create.CreateChildResult
import pl.sienkiewiczmaciej.routecrm.child.create.NewGuardianData
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.child.domain.DisabilityType
import pl.sienkiewiczmaciej.routecrm.child.domain.TransportNeeds
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildDetail
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildGuardianInfo
import pl.sienkiewiczmaciej.routecrm.child.list.ChildListItem
import pl.sienkiewiczmaciej.routecrm.child.update.UpdateChildCommand
import pl.sienkiewiczmaciej.routecrm.child.update.UpdateChildResult
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianRelationship
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate

data class TransportNeedsRequest(
    val wheelchair: Boolean = false,
    val specialSeat: Boolean = false,
    val safetyBelt: Boolean = false
) {
    fun toDomain() = TransportNeeds(
        wheelchair = wheelchair,
        specialSeat = specialSeat,
        safetyBelt = safetyBelt
    )
}

data class TransportNeedsResponse(
    val wheelchair: Boolean,
    val specialSeat: Boolean,
    val safetyBelt: Boolean
) {
    companion object {
        fun from(needs: TransportNeeds) = TransportNeedsResponse(
            wheelchair = needs.wheelchair,
            specialSeat = needs.specialSeat,
            safetyBelt = needs.safetyBelt
        )
    }
}

data class GuardianInfoRequest(
    val existingId: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val relationship: GuardianRelationship?
) {
    fun toNewGuardianData(): NewGuardianData? {
        if (existingId != null) return null
        require(firstName != null) { "Guardian first name is required" }
        require(lastName != null) { "Guardian last name is required" }
        require(email != null) { "Guardian email is required" }
        require(phone != null) { "Guardian phone is required" }
        require(relationship != null) { "Guardian relationship is required" }

        return NewGuardianData(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            relationship = relationship
        )
    }
}

data class CreateChildRequest(
    @field:Valid
    val child: ChildDataRequest,

    @field:Valid
    val guardian: GuardianInfoRequest
) {
    fun toCommand(companyId: CompanyId) = CreateChildCommand(
        companyId = companyId,
        firstName = child.firstName,
        lastName = child.lastName,
        birthDate = child.birthDate,
        disability = child.disability,
        transportNeeds = child.transportNeeds.toDomain(),
        notes = child.notes,
        guardianId = guardian.existingId?.let { GuardianId.from(it) },
        newGuardianData = guardian.toNewGuardianData()
    )
}

data class ChildDataRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 255)
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 255)
    val lastName: String,

    @field:NotNull(message = "Birth date is required")
    val birthDate: LocalDate,

    @field:NotEmpty(message = "At least one disability type is required")
    val disability: Set<DisabilityType>,

    @field:Valid
    val transportNeeds: TransportNeedsRequest,

    @field:Size(max = 5000)
    val notes: String?
)

data class ChildResponse(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val age: Int,
    val status: ChildStatus,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeedsResponse,
    val notes: String?,
    val guardiansCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: CreateChildResult, companyId: CompanyId) = ChildResponse(
            id = result.childId.value,
            companyId = companyId.value,
            firstName = result.child.firstName,
            lastName = result.child.lastName,
            birthDate = result.child.birthDate,
            age = result.child.age(),
            status = result.child.status,
            disability = result.child.disability,
            transportNeeds = TransportNeedsResponse.from(result.child.transportNeeds),
            notes = result.child.notes,
            guardiansCount = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

data class ChildListResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val age: Int,
    val status: ChildStatus,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeedsResponse,
    val guardiansCount: Int,
    val activeSchedulesCount: Int
) {
    companion object {
        fun from(item: ChildListItem) = ChildListResponse(
            id = item.id.value,
            firstName = item.firstName,
            lastName = item.lastName,
            birthDate = item.birthDate,
            age = item.age,
            status = item.status,
            disability = item.disability,
            transportNeeds = TransportNeedsResponse.from(item.transportNeeds),
            guardiansCount = item.guardiansCount,
            activeSchedulesCount = item.activeSchedulesCount
        )
    }
}

data class ChildDetailResponse(
    val id: String,
    val companyId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val age: Int,
    val status: ChildStatus,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeedsResponse,
    val notes: String?,
    val guardians: List<GuardianInfoResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(detail: ChildDetail) = ChildDetailResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
            firstName = detail.firstName,
            lastName = detail.lastName,
            birthDate = detail.birthDate,
            age = detail.age,
            status = detail.status,
            disability = detail.disability,
            transportNeeds = TransportNeedsResponse.from(detail.transportNeeds),
            notes = detail.notes,
            guardians = detail.guardians.map { GuardianInfoResponse.from(it) },
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

data class GuardianInfoResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String,
    val relationship: GuardianRelationship,
    val isPrimary: Boolean,
    val canPickup: Boolean,
    val canAuthorize: Boolean
) {
    companion object {
        fun from(info: ChildGuardianInfo) = GuardianInfoResponse(
            id = info.id,
            firstName = info.firstName,
            lastName = info.lastName,
            email = info.email,
            phone = info.phone,
            relationship = info.relationship,
            isPrimary = info.isPrimary,
            canPickup = info.canPickup,
            canAuthorize = info.canAuthorize
        )
    }
}

data class UpdateChildRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 255)
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 255)
    val lastName: String,

    @field:NotNull(message = "Birth date is required")
    val birthDate: LocalDate,

    @field:NotNull(message = "Status is required")
    val status: ChildStatus,

    @field:NotEmpty(message = "At least one disability type is required")
    val disability: Set<DisabilityType>,

    @field:Valid
    val transportNeeds: TransportNeedsRequest,

    @field:Size(max = 5000)
    val notes: String?
) {
    fun toCommand(companyId: CompanyId, id: ChildId) = UpdateChildCommand(
        companyId = companyId,
        id = id,
        firstName = firstName,
        lastName = lastName,
        birthDate = birthDate,
        status = status,
        disability = disability,
        transportNeeds = transportNeeds.toDomain(),
        notes = notes
    )
}

data class UpdateChildResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val status: ChildStatus,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateChildResult) = UpdateChildResponse(
            id = result.id.value,
            firstName = result.firstName,
            lastName = result.lastName,
            status = result.status,
            updatedAt = Instant.now()
        )
    }
}