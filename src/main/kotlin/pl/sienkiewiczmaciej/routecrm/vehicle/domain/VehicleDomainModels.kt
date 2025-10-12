package pl.sienkiewiczmaciej.routecrm.vehicle.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate
import java.util.*

@JvmInline
value class VehicleId(val value: String) {
    companion object {
        fun generate() = VehicleId("VEH-${UUID.randomUUID()}")
        fun from(value: String) = VehicleId(value)
    }
}

enum class VehicleType {
    BUS,
    MICROBUS,
    VAN
}

enum class VehicleStatus {
    AVAILABLE,
    IN_ROUTE,
    MAINTENANCE,
    OUT_OF_SERVICE
}

data class VehicleCapacity(
    val totalSeats: Int,
    val wheelchairSpaces: Int,
    val childSeats: Int
) {
    init {
        require(totalSeats in 1..50) { "Total seats must be between 1 and 50" }
        require(wheelchairSpaces >= 0) { "Wheelchair spaces cannot be negative" }
        require(childSeats >= 0) { "Child seats cannot be negative" }
        require(wheelchairSpaces <= totalSeats) { "Wheelchair spaces cannot exceed total seats" }
        require(childSeats <= totalSeats) { "Child seats cannot exceed total seats" }
    }
}

data class Insurance(
    val policyNumber: String,
    val validUntil: LocalDate,
    val insurer: String
) {
    init {
        require(policyNumber.isNotBlank()) { "Policy number is required" }
        require(insurer.isNotBlank()) { "Insurer is required" }
    }
}

data class TechnicalInspection(
    val validUntil: LocalDate,
    val inspectionStation: String
) {
    init {
        require(inspectionStation.isNotBlank()) { "Inspection station is required" }
    }
}

data class Vehicle(
    val id: VehicleId,
    val companyId: CompanyId,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int,
    val vehicleType: VehicleType,
    val capacity: VehicleCapacity,
    val specialEquipment: Set<String>,
    val insurance: Insurance?,
    val technicalInspection: TechnicalInspection?,
    val status: VehicleStatus,
    val currentMileage: Int,
    val vin: String?
) {
    companion object {
        private val REGISTRATION_NUMBER_REGEX = Regex("^[A-Z]{2,3}\\s?[0-9]{4,5}[A-Z]?$")

        fun create(
            companyId: CompanyId,
            registrationNumber: String,
            make: String,
            model: String,
            year: Int,
            vehicleType: VehicleType,
            capacity: VehicleCapacity,
            specialEquipment: Set<String>,
            insurance: Insurance?,
            technicalInspection: TechnicalInspection?,
            vin: String?
        ): Vehicle {
            require(registrationNumber.isNotBlank()) { "Registration number is required" }
            require(registrationNumber.uppercase().matches(REGISTRATION_NUMBER_REGEX)) {
                "Invalid registration number format"
            }
            require(make.isNotBlank()) { "Make is required" }
            require(make.length in 1..100) { "Make must be between 1 and 100 characters" }
            require(model.isNotBlank()) { "Model is required" }
            require(model.length in 1..100) { "Model must be between 1 and 100 characters" }
            require(year in 1990..(LocalDate.now().year + 1)) {
                "Year must be between 1990 and ${LocalDate.now().year + 1}"
            }

            return Vehicle(
                id = VehicleId.generate(),
                companyId = companyId,
                registrationNumber = registrationNumber.uppercase().trim(),
                make = make.trim(),
                model = model.trim(),
                year = year,
                vehicleType = vehicleType,
                capacity = capacity,
                specialEquipment = specialEquipment,
                insurance = insurance,
                technicalInspection = technicalInspection,
                status = VehicleStatus.AVAILABLE,
                currentMileage = 0,
                vin = vin?.trim()
            )
        }
    }

    fun update(
        registrationNumber: String,
        status: VehicleStatus,
        currentMileage: Int,
        insurance: Insurance?,
        technicalInspection: TechnicalInspection?
    ): Vehicle {
        require(registrationNumber.isNotBlank()) { "Registration number is required" }
        require(currentMileage >= 0) { "Mileage cannot be negative" }
        require(currentMileage >= this.currentMileage) { "Mileage cannot decrease" }

        return copy(
            registrationNumber = registrationNumber.uppercase().trim(),
            status = status,
            currentMileage = currentMileage,
            insurance = insurance,
            technicalInspection = technicalInspection
        )
    }
}