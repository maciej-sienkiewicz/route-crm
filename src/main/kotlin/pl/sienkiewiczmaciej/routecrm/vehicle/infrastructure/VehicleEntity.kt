package pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "vehicles",
    indexes = [
        Index(name = "idx_vehicles_company", columnList = "company_id"),
        Index(name = "idx_vehicles_company_status", columnList = "company_id, status"),
        Index(name = "idx_vehicles_company_type", columnList = "company_id, vehicle_type"),
        Index(name = "idx_vehicles_company_reg", columnList = "company_id, registration_number", unique = true)
    ]
)
class VehicleEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "registration_number", nullable = false, length = 20)
    val registrationNumber: String,

    @Column(nullable = false, length = 100)
    val make: String,

    @Column(nullable = false, length = 100)
    val model: String,

    @Column(nullable = false)
    val year: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    val vehicleType: VehicleType,

    @Column(name = "capacity_total_seats", nullable = false)
    val capacityTotalSeats: Int,

    @Column(name = "capacity_wheelchair_spaces", nullable = false)
    val capacityWheelchairSpaces: Int,

    @Column(name = "capacity_child_seats", nullable = false)
    val capacityChildSeats: Int,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_equipment", columnDefinition = "jsonb", nullable = false)
    val specialEquipment: Set<String>,

    @Column(name = "insurance_policy_number", length = 100)
    val insurancePolicyNumber: String?,

    @Column(name = "insurance_valid_until")
    val insuranceValidUntil: LocalDate?,

    @Column(name = "insurance_insurer", length = 255)
    val insuranceInsurer: String?,

    @Column(name = "technical_inspection_valid_until")
    val technicalInspectionValidUntil: LocalDate?,

    @Column(name = "technical_inspection_station", length = 255)
    val technicalInspectionStation: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: VehicleStatus,

    @Column(name = "current_mileage", nullable = false)
    val currentMileage: Int,

    @Column(length = 17)
    val vin: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = Vehicle(
        id = VehicleId(id),
        companyId = CompanyId(companyId),
        registrationNumber = registrationNumber,
        make = make,
        model = model,
        year = year,
        vehicleType = vehicleType,
        capacity = VehicleCapacity(
            totalSeats = capacityTotalSeats,
            wheelchairSpaces = capacityWheelchairSpaces,
            childSeats = capacityChildSeats
        ),
        specialEquipment = specialEquipment,
        insurance = run {
            val policyNumber = insurancePolicyNumber
            val validUntil = insuranceValidUntil
            val insurer = insuranceInsurer
            if (policyNumber != null && validUntil != null && insurer != null) {
                Insurance(
                    policyNumber = policyNumber,
                    validUntil = validUntil,
                    insurer = insurer
                )
            } else null
        },
        technicalInspection = run {
            val validUntil = technicalInspectionValidUntil
            val station = technicalInspectionStation
            if (validUntil != null && station != null) {
                TechnicalInspection(
                    validUntil = validUntil,
                    inspectionStation = station
                )
            } else null
        },
        status = status,
        currentMileage = currentMileage,
        vin = vin
    )

    companion object {
        fun fromDomain(vehicle: Vehicle) = VehicleEntity(
            id = vehicle.id.value,
            companyId = vehicle.companyId.value,
            registrationNumber = vehicle.registrationNumber,
            make = vehicle.make,
            model = vehicle.model,
            year = vehicle.year,
            vehicleType = vehicle.vehicleType,
            capacityTotalSeats = vehicle.capacity.totalSeats,
            capacityWheelchairSpaces = vehicle.capacity.wheelchairSpaces,
            capacityChildSeats = vehicle.capacity.childSeats,
            specialEquipment = vehicle.specialEquipment,
            insurancePolicyNumber = vehicle.insurance?.policyNumber,
            insuranceValidUntil = vehicle.insurance?.validUntil,
            insuranceInsurer = vehicle.insurance?.insurer,
            technicalInspectionValidUntil = vehicle.technicalInspection?.validUntil,
            technicalInspectionStation = vehicle.technicalInspection?.inspectionStation,
            status = vehicle.status,
            currentMileage = vehicle.currentMileage,
            vin = vehicle.vin
        )
    }
}