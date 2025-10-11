package pl.sienkiewiczmaciej.routecrm.child.infrastructure

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.child.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "children",
    indexes = [
        Index(name = "idx_children_company", columnList = "company_id"),
        Index(name = "idx_children_company_status", columnList = "company_id, status"),
        Index(name = "idx_children_company_name", columnList = "company_id, last_name, first_name")
    ]
)
class ChildEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "first_name", nullable = false, length = 255)
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 255)
    val lastName: String,

    @Column(name = "birth_date", nullable = false)
    val birthDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val status: ChildStatus,

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = DisabilitySetConverter::class)
    val disability: Set<DisabilityType>,

    @Column(name = "transport_needs", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = TransportNeedsConverter::class)
    val transportNeeds: TransportNeeds,

    @Column(columnDefinition = "text")
    val notes: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = Child(
        id = ChildId(id),
        companyId = CompanyId(companyId),
        firstName = firstName,
        lastName = lastName,
        birthDate = birthDate,
        disability = disability,
        transportNeeds = transportNeeds,
        notes = notes,
        status = status
    )

    companion object {
        fun fromDomain(child: Child) = ChildEntity(
            id = child.id.value,
            companyId = child.companyId.value,
            firstName = child.firstName,
            lastName = child.lastName,
            birthDate = child.birthDate,
            status = child.status,
            disability = child.disability,
            transportNeeds = child.transportNeeds,
            notes = child.notes
        )
    }
}

@Converter
class DisabilitySetConverter : AttributeConverter<Set<DisabilityType>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Set<DisabilityType>?): String {
        return objectMapper.writeValueAsString(attribute ?: emptySet<DisabilityType>())
    }

    override fun convertToEntityAttribute(dbData: String?): Set<DisabilityType> {
        if (dbData.isNullOrBlank()) return emptySet()
        return objectMapper.readValue(dbData, object : TypeReference<Set<DisabilityType>>() {})
    }
}

@Converter
class TransportNeedsConverter : AttributeConverter<TransportNeeds, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: TransportNeeds?): String {
        return objectMapper.writeValueAsString(attribute ?: TransportNeeds(false, false, false))
    }

    override fun convertToEntityAttribute(dbData: String?): TransportNeeds {
        if (dbData.isNullOrBlank()) return TransportNeeds(false, false, false)
        return objectMapper.readValue(dbData, TransportNeeds::class.java)
    }
}