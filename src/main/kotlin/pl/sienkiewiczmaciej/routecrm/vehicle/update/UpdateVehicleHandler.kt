package pl.sienkiewiczmaciej.routecrm.vehicle.update

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*
import pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.VehicleNotFoundException

data class UpdateVehicleCommand(
    val companyId: CompanyId,
    val id: VehicleId,
    val registrationNumber: String,
    val status: VehicleStatus,
    val currentMileage: Int,
    val insurance: Insurance?,
    val technicalInspection: TechnicalInspection?
)

data class UpdateVehicleResult(
    val id: VehicleId,
    val registrationNumber: String,
    val status: VehicleStatus,
    val currentMileage: Int
)

@Component
class UpdateVehicleHandler(
    private val vehicleRepository: VehicleRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateVehicleCommand): UpdateVehicleResult {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val vehicle = vehicleRepository.findById(command.companyId, command.id)
            ?: throw VehicleNotFoundException(command.id)

        if (vehicleRepository.existsByRegistrationNumberExcludingId(
                command.companyId,
                command.registrationNumber,
                command.id
            )
        ) {
            throw IllegalArgumentException("Vehicle with registration number ${command.registrationNumber} already exists")
        }

        val updated = vehicle.update(
            registrationNumber = command.registrationNumber,
            status = command.status,
            currentMileage = command.currentMileage,
            insurance = command.insurance,
            technicalInspection = command.technicalInspection
        )

        val saved = vehicleRepository.save(updated)

        return UpdateVehicleResult(
            id = saved.id,
            registrationNumber = saved.registrationNumber,
            status = saved.status,
            currentMileage = saved.currentMileage
        )
    }
}