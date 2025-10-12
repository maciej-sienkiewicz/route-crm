package pl.sienkiewiczmaciej.routecrm.vehicle.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*

data class CreateVehicleCommand(
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
    val vin: String?
)

data class CreateVehicleResult(
    val id: VehicleId,
    val companyId: CompanyId,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int,
    val vehicleType: VehicleType,
    val status: VehicleStatus
)

@Component
class CreateVehicleHandler(
    private val vehicleRepository: VehicleRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateVehicleCommand): CreateVehicleResult {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        if (vehicleRepository.existsByRegistrationNumber(command.companyId, command.registrationNumber)) {
            throw IllegalArgumentException("Vehicle with registration number ${command.registrationNumber} already exists")
        }

        val vehicle = Vehicle.create(
            companyId = command.companyId,
            registrationNumber = command.registrationNumber,
            make = command.make,
            model = command.model,
            year = command.year,
            vehicleType = command.vehicleType,
            capacity = command.capacity,
            specialEquipment = command.specialEquipment,
            insurance = command.insurance,
            technicalInspection = command.technicalInspection,
            vin = command.vin
        )

        val saved = vehicleRepository.save(vehicle)

        return CreateVehicleResult(
            id = saved.id,
            companyId = saved.companyId,
            registrationNumber = saved.registrationNumber,
            make = saved.make,
            model = saved.model,
            year = saved.year,
            vehicleType = saved.vehicleType,
            status = saved.status
        )
    }
}