package pl.sienkiewiczmaciej.routecrm.schedule.infrastructure

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalTime

@Entity
@Table(
    name = "schedules",
    indexes = [
        Index(name = "idx_schedules_company", columnList = "company_id"),
        Index(name = "idx_schedules_child", columnList = "company_id, child_id"),
        Index(name = "idx_schedules_active", columnList = "company_id, active")
    ]
)
class ScheduleEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "child_id", nullable = false, length = 50)
    val childId: String,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = DayOfWeekSetConverter::class)
    val days: Set<DayOfWeek>,

    @Column(name = "pickup_time", nullable = false)
    val pickupTime: LocalTime,

    @Column(name = "pickup_address_label", length = 100)
    val pickupAddressLabel: String?,

    @Column(name = "pickup_address_street", nullable = false, length = 255)
    val pickupAddressStreet: String,

    @Column(name = "pickup_address_house_number", nullable = false, length = 20)
    val pickupAddressHouseNumber: String,

    @Column(name = "pickup_address_apartment_number", length = 20)
    val pickupAddressApartmentNumber: String?,

    @Column(name = "pickup_address_postal_code", nullable = false, length = 10)
    val pickupAddressPostalCode: String,

    @Column(name = "pickup_address_city", nullable = false, length = 100)
    val pickupAddressCity: String,

    @Column(name = "dropoff_time", nullable = false)
    val dropoffTime: LocalTime,

    @Column(name = "dropoff_address_label", length = 100)
    val dropoffAddressLabel: String?,

    @Column(name = "dropoff_address_street", nullable = false, length = 255)
    val dropoffAddressStreet: String,

    @Column(name = "dropoff_address_house_number", nullable = false, length = 20)
    val dropoffAddressHouseNumber: String,

    @Column(name = "dropoff_address_apartment_number", length = 20)
    val dropoffAddressApartmentNumber: String?,

    @Column(name = "dropoff_address_postal_code", nullable = false, length = 10)
    val dropoffAddressPostalCode: String,

    @Column(name = "dropoff_address_city", nullable = false, length = 100)
    val dropoffAddressCity: String,

    @Column(name = "special_instructions", columnDefinition = "text")
    val specialInstructions: String?,

    @Column(nullable = false)
    val active: Boolean,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = Schedule(
        id = ScheduleId(id),
        companyId = CompanyId(companyId),
        childId = ChildId(childId),
        name = name,
        days = days,
        pickupTime = pickupTime,
        pickupAddress = ScheduleAddress(
            label = pickupAddressLabel,
            address = Address(
                street = pickupAddressStreet,
                houseNumber = pickupAddressHouseNumber,
                apartmentNumber = pickupAddressApartmentNumber,
                postalCode = pickupAddressPostalCode,
                city = pickupAddressCity
            )
        ),
        dropoffTime = dropoffTime,
        dropoffAddress = ScheduleAddress(
            label = dropoffAddressLabel,
            address = Address(
                street = dropoffAddressStreet,
                houseNumber = dropoffAddressHouseNumber,
                apartmentNumber = dropoffAddressApartmentNumber,
                postalCode = dropoffAddressPostalCode,
                city = dropoffAddressCity
            )
        ),
        specialInstructions = specialInstructions,
        active = active
    )

    companion object {
        fun fromDomain(schedule: Schedule) = ScheduleEntity(
            id = schedule.id.value,
            companyId = schedule.companyId.value,
            childId = schedule.childId.value,
            name = schedule.name,
            days = schedule.days,
            pickupTime = schedule.pickupTime,
            pickupAddressLabel = schedule.pickupAddress.label,
            pickupAddressStreet = schedule.pickupAddress.address.street,
            pickupAddressHouseNumber = schedule.pickupAddress.address.houseNumber,
            pickupAddressApartmentNumber = schedule.pickupAddress.address.apartmentNumber,
            pickupAddressPostalCode = schedule.pickupAddress.address.postalCode,
            pickupAddressCity = schedule.pickupAddress.address.city,
            dropoffTime = schedule.dropoffTime,
            dropoffAddressLabel = schedule.dropoffAddress.label,
            dropoffAddressStreet = schedule.dropoffAddress.address.street,
            dropoffAddressHouseNumber = schedule.dropoffAddress.address.houseNumber,
            dropoffAddressApartmentNumber = schedule.dropoffAddress.address.apartmentNumber,
            dropoffAddressPostalCode = schedule.dropoffAddress.address.postalCode,
            dropoffAddressCity = schedule.dropoffAddress.address.city,
            specialInstructions = schedule.specialInstructions,
            active = schedule.active
        )
    }
}

@Converter
class DayOfWeekSetConverter : AttributeConverter<Set<DayOfWeek>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Set<DayOfWeek>?): String {
        return objectMapper.writeValueAsString(attribute ?: emptySet<DayOfWeek>())
    }

    override fun convertToEntityAttribute(dbData: String?): Set<DayOfWeek> {
        if (dbData.isNullOrBlank()) return emptySet()
        return objectMapper.readValue(dbData, object : TypeReference<Set<DayOfWeek>>() {})
    }
}