package pl.sienkiewiczmaciej.routecrm.schedule

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.create.CreateScheduleCommand
import pl.sienkiewiczmaciej.routecrm.schedule.create.CreateScheduleResult
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.ScheduleDetail
import pl.sienkiewiczmaciej.routecrm.schedule.list.ScheduleListItem
import pl.sienkiewiczmaciej.routecrm.schedule.update.UpdateScheduleCommand
import pl.sienkiewiczmaciej.routecrm.schedule.update.UpdateScheduleResult
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@JsonIgnoreProperties
data class ScheduleAddressRequest(
    @field:Size(max = 100)
    val label: String?,

    @field:Valid
    val street: String,

    @field:Valid
    val houseNumber: String,

    val apartmentNumber: String?,

    @field:Valid
    val postalCode: String,

    @field:Valid
    val city: String,

    @field:Valid
    val latitude: Double,

    @field:Valid
    val longitude: Double
) {
    fun toDomainAddress() = pl.sienkiewiczmaciej.routecrm.shared.domain.Address(
        street = street.trim(),
        houseNumber = houseNumber.trim(),
        apartmentNumber = apartmentNumber?.trim(),
        postalCode = postalCode.trim(),
        city = city.trim()
    )
}

data class ScheduleAddressResponse(
    val label: String?,
    val street: String,
    val houseNumber: String,
    val apartmentNumber: String?,
    val postalCode: String,
    val city: String,
    val latitude: Double?,
    val longitude: Double?
) {
    companion object {
        fun from(scheduleAddress: ScheduleAddress) = ScheduleAddressResponse(
            label = scheduleAddress.label,
            street = scheduleAddress.address.street,
            houseNumber = scheduleAddress.address.houseNumber,
            apartmentNumber = scheduleAddress.address.apartmentNumber,
            postalCode = scheduleAddress.address.postalCode,
            city = scheduleAddress.address.city,
            latitude = scheduleAddress.latitude,
            longitude = scheduleAddress.longitude
        )
    }
}

data class CreateScheduleRequest(
    @field:NotBlank(message = "Schedule name is required")
    @field:Size(min = 1, max = 255)
    val name: String,

    @field:NotEmpty(message = "At least one day is required")
    val days: Set<DayOfWeek>,

    @field:NotNull(message = "Pickup time is required")
    val pickupTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Pickup address is required")
    val pickupAddress: ScheduleAddressRequest,

    @field:NotNull(message = "Dropoff time is required")
    val dropoffTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Dropoff address is required")
    val dropoffAddress: ScheduleAddressRequest,

    @field:Size(max = 5000)
    val specialInstructions: String?
) {
    fun toCommand(companyId: CompanyId, childId: ChildId) = CreateScheduleCommand(
        companyId = companyId,
        childId = childId,
        name = name,
        days = days,
        pickupTime = pickupTime,
        pickupAddressData = pickupAddress.toDomainAddress(),
        pickupAddressLabel = pickupAddress.label,
        dropoffTime = dropoffTime,
        dropoffAddressData = dropoffAddress.toDomainAddress(),
        dropoffAddressLabel = dropoffAddress.label,
        specialInstructions = specialInstructions
    )
}

data class ScheduleResponse(
    val id: String,
    val childId: String,
    val companyId: String,
    val name: String,
    val days: Set<DayOfWeek>,
    @JsonFormat(pattern = "HH:mm")
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddressResponse,
    @JsonFormat(pattern = "HH:mm")
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddressResponse,
    val specialInstructions: String?,
    val active: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(result: CreateScheduleResult) = ScheduleResponse(
            id = result.id.value,
            childId = result.childId.value,
            companyId = result.companyId.value,
            name = result.name,
            days = result.days,
            pickupTime = result.pickupTime,
            pickupAddress = ScheduleAddressResponse.from(result.pickupAddress),  // ← Ma już współrzędne!
            dropoffTime = result.dropoffTime,
            dropoffAddress = ScheduleAddressResponse.from(result.dropoffAddress), // ← Ma już współrzędne!
            specialInstructions = result.specialInstructions,
            active = result.active,
            createdAt = Instant.now()
        )
    }
}

data class ScheduleListResponse(
    val id: String,
    val name: String,
    val days: Set<DayOfWeek>,
    @JsonFormat(pattern = "HH:mm")
    val pickupTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val dropoffTime: LocalTime,
    val active: Boolean
) {
    companion object {
        fun from(item: ScheduleListItem) = ScheduleListResponse(
            id = item.id.value,
            name = item.name,
            days = item.days,
            pickupTime = item.pickupTime,
            dropoffTime = item.dropoffTime,
            active = item.active
        )
    }
}

data class ScheduleDetailResponse(
    val id: String,
    val companyId: String,
    val childId: String,
    val name: String,
    val days: Set<DayOfWeek>,
    @JsonFormat(pattern = "HH:mm")
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddressResponse,
    @JsonFormat(pattern = "HH:mm")
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddressResponse,
    val specialInstructions: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(detail: ScheduleDetail) = ScheduleDetailResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
            childId = detail.childId.value,
            name = detail.name,
            days = detail.days,
            pickupTime = detail.pickupTime,
            pickupAddress = ScheduleAddressResponse.from(detail.pickupAddress),
            dropoffTime = detail.dropoffTime,
            dropoffAddress = ScheduleAddressResponse.from(detail.dropoffAddress),
            specialInstructions = detail.specialInstructions,
            active = detail.active,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

data class UpdateScheduleRequest(
    @field:NotBlank(message = "Schedule name is required")
    @field:Size(min = 1, max = 255)
    val name: String,

    @field:NotEmpty(message = "At least one day is required")
    val days: Set<DayOfWeek>,

    @field:NotNull(message = "Pickup time is required")
    val pickupTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Pickup address is required")
    val pickupAddress: ScheduleAddressRequest,

    @field:NotNull(message = "Dropoff time is required")
    val dropoffTime: LocalTime,

    @field:Valid
    @field:NotNull(message = "Dropoff address is required")
    val dropoffAddress: ScheduleAddressRequest,

    @field:Size(max = 5000)
    val specialInstructions: String?,

    @field:NotNull(message = "Active status is required")
    val active: Boolean
) {
    fun toCommand(companyId: CompanyId, id: ScheduleId) = UpdateScheduleCommand(
        companyId = companyId,
        id = id,
        name = name,
        days = days,
        pickupTime = pickupTime,
        pickupAddressData = pickupAddress.toDomainAddress(),
        pickupAddressLabel = pickupAddress.label,
        dropoffTime = dropoffTime,
        dropoffAddressData = dropoffAddress.toDomainAddress(),
        dropoffAddressLabel = dropoffAddress.label,
        specialInstructions = specialInstructions,
        active = active
    )
}

data class UpdateScheduleResponse(
    val id: String,
    val name: String,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateScheduleResult) = UpdateScheduleResponse(
            id = result.id.value,
            name = result.name,
            updatedAt = Instant.now()
        )
    }
}

data class SchedulesListResponse(
    val schedules: List<ScheduleListResponse>
) {
    companion object {
        fun from(schedules: List<ScheduleListItem>) = SchedulesListResponse(
            schedules = schedules.map { ScheduleListResponse.from(it) }
        )
    }
}

data class UnassignedScheduleItem(
    val scheduleId: String,
    val childId: String,
    val childFirstName: String,
    val childLastName: String,
    val scheduleName: String,
    val days: Set<DayOfWeek>,
    @JsonFormat(pattern = "HH:mm")
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddressResponse,
    @JsonFormat(pattern = "HH:mm")
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddressResponse,
    val specialInstructions: String?
) {
    companion object {
        fun from(item: pl.sienkiewiczmaciej.routecrm.schedule.findunassigned.UnassignedScheduleItem) =
            UnassignedScheduleItem(
                scheduleId = item.scheduleId.value,
                childId = item.childId.value,
                childFirstName = item.childFirstName,
                childLastName = item.childLastName,
                scheduleName = item.scheduleName,
                days = item.days,
                pickupTime = item.pickupTime,
                pickupAddress = ScheduleAddressResponse.from(item.pickupAddress),
                dropoffTime = item.dropoffTime,
                dropoffAddress = ScheduleAddressResponse.from(item.dropoffAddress),
                specialInstructions = item.specialInstructions
            )
    }
}

data class UnassignedSchedulesResponse(
    val date: LocalDate,
    val schedules: List<UnassignedScheduleItem>,
    val totalCount: Int
) {
    companion object {
        fun from(result: pl.sienkiewiczmaciej.routecrm.schedule.findunassigned.UnassignedSchedulesResult) =
            UnassignedSchedulesResponse(
                date = result.date,
                schedules = result.schedules.map { UnassignedScheduleItem.from(it) },
                totalCount = result.schedules.size
            )
    }
}