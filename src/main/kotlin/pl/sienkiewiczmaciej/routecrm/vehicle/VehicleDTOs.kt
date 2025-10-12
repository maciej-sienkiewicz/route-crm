package pl.sienkiewiczmaciej.routecrm.vehicle

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.create.CreateVehicleCommand
import pl.sienkiewiczmaciej.routecrm.vehicle.create.CreateVehicleResult
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*
import pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.VehicleDetail
import pl.sienkiewiczmaciej.routecrm.vehicle.list.VehicleListItem
import pl.sienkiewiczmaciej.routecrm.vehicle.update.UpdateVehicleCommand
import pl.sienkiewiczmaciej.routecrm.vehicle.update.UpdateVehicleResult
import java.time.Instant
import java.time.LocalDate

data class VehicleCapacityRequest(
    @field:Min(1, message = "Total seats must be at least 1")
    @field:Max(50, message = "Total seats cannot exceed 50")
    val totalSeats: Int,

    @field:Min(0, message = "Wheelchair spaces cannot be negative")
    val wheelchairSpaces: Int,

    @field:Min(0, message = "Child seats cannot be negative")
    val childSeats: Int
) {
    fun toDomain() = VehicleCapacity(
        totalSeats = totalSeats,
        wheelchairSpaces = wheelchairSpaces,
        childSeats = childSeats
    )
}

data class VehicleCapacityResponse(
    val totalSeats: Int,
    val wheelchairSpaces: Int,
    val childSeats: Int
) {
    companion object {
        fun from(capacity: VehicleCapacity) = VehicleCapacityResponse(
            totalSeats = capacity.totalSeats,
            wheelchairSpaces = capacity.wheelchairSpaces,
            childSeats = capacity.childSeats
        )
    }
}

data class InsuranceRequest(
    @field:NotBlank(message = "Policy number is required")
    @field:Size(max = 100)
    val policyNumber: String,

    @field:NotNull(message = "Valid until date is required")
    val validUntil: LocalDate,

    @field:NotBlank(message = "Insurer is required")
    @field:Size(max = 255)
    val insurer: String
) {
    fun toDomain() = Insurance(
        policyNumber = policyNumber.trim(),
        validUntil = validUntil,
        insurer = insurer.trim()
    )
}

data class InsuranceResponse(
    val policyNumber: String,
    val validUntil: LocalDate,
    val insurer: String
) {
    companion object {
        fun from(insurance: Insurance) = InsuranceResponse(
            policyNumber = insurance.policyNumber,
            validUntil = insurance.validUntil,
            insurer = insurance.insurer
        )
    }
}

data class TechnicalInspectionRequest(
    @field:NotNull(message = "Valid until date is required")
    val validUntil: LocalDate,

    @field:NotBlank(message = "Inspection station is required")
    @field:Size(max = 255)
    val inspectionStation: String
) {
    fun toDomain() = TechnicalInspection(
        validUntil = validUntil,
        inspectionStation = inspectionStation.trim()
    )
}

data class TechnicalInspectionResponse(
    val validUntil: LocalDate,
    val inspectionStation: String
) {
    companion object {
        fun from(inspection: TechnicalInspection) = TechnicalInspectionResponse(
            validUntil = inspection.validUntil,
            inspectionStation = inspection.inspectionStation
        )
    }
}

data class CreateVehicleRequest(
    @field:NotBlank(message = "Registration number is required")
    @field:Pattern(regexp = "^[A-Z]{2,3}\\s?[0-9]{4,5}[A-Z]?$", message = "Invalid registration number format")
    val registrationNumber: String,

    @field:NotBlank(message = "Make is required")
    @field:Size(min = 1, max = 100)
    val make: String,

    @field:NotBlank(message = "Model is required")
    @field:Size(min = 1, max = 100)
    val model: String,

    @field:NotNull(message = "Year is required")
    @field:Min(1990, message = "Year must be 1990 or later")
    val year: Int,

    @field:NotNull(message = "Vehicle type is required")
    val vehicleType: VehicleType,

    @field:Valid
    @field:NotNull(message = "Capacity is required")
    val capacity: VehicleCapacityRequest,

    val specialEquipment: Set<String> = emptySet(),

    @field:Valid
    val insurance: InsuranceRequest?,

    @field:Valid
    val technicalInspection: TechnicalInspectionRequest?,

    @field:Size(max = 17)
    val vin: String? = null
) {
    fun toCommand(companyId: CompanyId) = CreateVehicleCommand(
        companyId = companyId,
        registrationNumber = registrationNumber,
        make = make,
        model = model,
        year = year,
        vehicleType = vehicleType,
        capacity = capacity.toDomain(),
        specialEquipment = specialEquipment,
        insurance = insurance?.toDomain(),
        technicalInspection = technicalInspection?.toDomain(),
        vin = vin
    )
}

data class VehicleResponse(
    val id: String,
    val companyId: String,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int,
    val vehicleType: VehicleType,
    val capacity: VehicleCapacityResponse,
    val specialEquipment: Set<String>,
    val insurance: InsuranceResponse?,
    val technicalInspection: TechnicalInspectionResponse?,
    val status: VehicleStatus,
    val currentMileage: Int,
    val vin: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(result: CreateVehicleResult, command: CreateVehicleCommand) = VehicleResponse(
            id = result.id.value,
            companyId = result.companyId.value,
            registrationNumber = result.registrationNumber,
            make = result.make,
            model = result.model,
            year = result.year,
            vehicleType = result.vehicleType,
            capacity = VehicleCapacityResponse.from(command.capacity),
            specialEquipment = command.specialEquipment,
            insurance = command.insurance?.let { InsuranceResponse.from(it) },
            technicalInspection = command.technicalInspection?.let { TechnicalInspectionResponse.from(it) },
            status = result.status,
            currentMileage = 0,
            vin = command.vin,
            createdAt = Instant.now()
        )
    }
}

data class VehicleListResponse(
    val id: String,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int,
    val vehicleType: VehicleType,
    val status: VehicleStatus,
    val capacity: VehicleCapacitySimpleResponse,
    val insurance: InsuranceSimpleResponse,
    val technicalInspection: TechnicalInspectionSimpleResponse
) {
    companion object {
        fun from(item: VehicleListItem) = VehicleListResponse(
            id = item.id.value,
            registrationNumber = item.registrationNumber,
            make = item.make,
            model = item.model,
            year = item.year,
            vehicleType = item.vehicleType,
            status = item.status,
            capacity = VehicleCapacitySimpleResponse(
                totalSeats = item.totalSeats,
                wheelchairSpaces = item.wheelchairSpaces
            ),
            insurance = InsuranceSimpleResponse(
                validUntil = item.insuranceValidUntil
            ),
            technicalInspection = TechnicalInspectionSimpleResponse(
                validUntil = item.technicalInspectionValidUntil
            )
        )
    }
}

data class VehicleCapacitySimpleResponse(
    val totalSeats: Int,
    val wheelchairSpaces: Int
)

data class InsuranceSimpleResponse(
    val validUntil: LocalDate?
)

data class TechnicalInspectionSimpleResponse(
    val validUntil: LocalDate?
)

data class VehicleDetailResponse(
    val id: String,
    val companyId: String,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int,
    val vehicleType: VehicleType,
    val capacity: VehicleCapacityResponse,
    val specialEquipment: Set<String>,
    val insurance: InsuranceResponse?,
    val technicalInspection: TechnicalInspectionResponse?,
    val status: VehicleStatus,
    val currentMileage: Int,
    val vin: String?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(detail: VehicleDetail) = VehicleDetailResponse(
            id = detail.id.value,
            companyId = detail.companyId.value,
            registrationNumber = detail.registrationNumber,
            make = detail.make,
            model = detail.model,
            year = detail.year,
            vehicleType = detail.vehicleType,
            capacity = VehicleCapacityResponse.from(detail.capacity),
            specialEquipment = detail.specialEquipment,
            insurance = detail.insurance?.let { InsuranceResponse.from(it) },
            technicalInspection = detail.technicalInspection?.let { TechnicalInspectionResponse.from(it) },
            status = detail.status,
            currentMileage = detail.currentMileage,
            vin = detail.vin,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

data class UpdateVehicleRequest(
    @field:NotBlank(message = "Registration number is required")
    @field:Pattern(regexp = "^[A-Z]{2,3}\\s?[0-9]{4,5}[A-Z]?$", message = "Invalid registration number format")
    val registrationNumber: String,

    @field:NotNull(message = "Status is required")
    val status: VehicleStatus,

    @field:NotNull(message = "Current mileage is required")
    @field:Min(0, message = "Mileage cannot be negative")
    val currentMileage: Int,

    @field:Valid
    val insurance: InsuranceRequest?,

    @field:Valid
    val technicalInspection: TechnicalInspectionRequest?
) {
    fun toCommand(companyId: CompanyId, id: VehicleId) = UpdateVehicleCommand(
        companyId = companyId,
        id = id,
        registrationNumber = registrationNumber,
        status = status,
        currentMileage = currentMileage,
        insurance = insurance?.toDomain(),
        technicalInspection = technicalInspection?.toDomain()
    )
}

data class UpdateVehicleResponse(
    val id: String,
    val registrationNumber: String,
    val status: VehicleStatus,
    val currentMileage: Int,
    val updatedAt: Instant
) {
    companion object {
        fun from(result: UpdateVehicleResult) = UpdateVehicleResponse(
            id = result.id.value,
            registrationNumber = result.registrationNumber,
            status = result.status,
            currentMileage = result.currentMileage,
            updatedAt = Instant.now()
        )
    }
}