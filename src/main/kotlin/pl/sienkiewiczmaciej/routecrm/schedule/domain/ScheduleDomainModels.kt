package pl.sienkiewiczmaciej.routecrm.schedule.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalTime
import java.util.*

@JvmInline
value class ScheduleId(val value: String) {
    companion object {
        fun generate() = ScheduleId("SCH-${UUID.randomUUID()}")
        fun from(value: String) = ScheduleId(value)
    }
}

enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

data class ScheduleAddress(
    val label: String?,
    val address: Address,
    val latitude: Double?,
    val longitude: Double?
)

data class Schedule(
    val id: ScheduleId,
    val companyId: CompanyId,
    val childId: ChildId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddress,
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddress,
    val specialInstructions: String?,
    val active: Boolean
) {
    companion object {
        fun create(
            companyId: CompanyId,
            childId: ChildId,
            name: String,
            days: Set<DayOfWeek>,
            pickupTime: LocalTime,
            pickupAddress: ScheduleAddress,
            dropoffTime: LocalTime,
            dropoffAddress: ScheduleAddress,
            specialInstructions: String?
        ): Schedule {
            require(name.isNotBlank()) { "Schedule name is required" }
            require(days.isNotEmpty()) { "At least one day is required" }
            require(dropoffTime.isAfter(pickupTime)) { "Dropoff time must be after pickup time" }

            return Schedule(
                id = ScheduleId.generate(),
                companyId = companyId,
                childId = childId,
                name = name.trim(),
                days = days,
                pickupTime = pickupTime,
                pickupAddress = pickupAddress,
                dropoffTime = dropoffTime,
                dropoffAddress = dropoffAddress,
                specialInstructions = specialInstructions?.trim(),
                active = true
            )
        }
    }

    fun update(
        name: String,
        days: Set<DayOfWeek>,
        pickupTime: LocalTime,
        pickupAddress: ScheduleAddress,
        dropoffTime: LocalTime,
        dropoffAddress: ScheduleAddress,
        specialInstructions: String?,
        active: Boolean
    ): Schedule {
        require(name.isNotBlank()) { "Schedule name is required" }
        require(days.isNotEmpty()) { "At least one day is required" }
        require(dropoffTime.isAfter(pickupTime)) { "Dropoff time must be after pickup time" }

        return copy(
            name = name.trim(),
            days = days,
            pickupTime = pickupTime,
            pickupAddress = pickupAddress,
            dropoffTime = dropoffTime,
            dropoffAddress = dropoffAddress,
            specialInstructions = specialInstructions?.trim(),
            active = active
        )
    }
}