package pl.sienkiewiczmaciej.routecrm.child.infrastructure

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    val disability: Set<DisabilityType>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transport_needs", columnDefinition = "jsonb", nullable = false)
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
