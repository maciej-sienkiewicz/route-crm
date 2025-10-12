package pl.sienkiewiczmaciej.routecrm.vehicle.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*

data class GetVehicleQuery(
    val companyId: CompanyId,
    val id: VehicleId
)

data class VehicleDetail(
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
)

class VehicleNotFoundException(id: VehicleId) : NotFoundException("Vehicle ${id.value} not found")

@Component
class GetVehicleHandler(
    private val vehicleRepository: VehicleRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetVehicleQuery): VehicleDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val vehicle = vehicleRepository.findById(query.companyId, query.id)
            ?: throw VehicleNotFoundException(query.id)

        return VehicleDetail(
            id = vehicle.id,
            companyId = vehicle.companyId,
            registrationNumber = vehicle.registrationNumber,
            make = vehicle.make,
            model = vehicle.model,
            year = vehicle.year,
            vehicleType = vehicle.vehicleType,
            capacity = vehicle.capacity,
            specialEquipment = vehicle.specialEquipment,
            insurance = vehicle.insurance,
            technicalInspection = vehicle.technicalInspection,
            status = vehicle.status,
            currentMileage = vehicle.currentMileage,
            vin = vehicle.vin
        )
    }
}