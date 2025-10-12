package pl.sienkiewiczmaciej.routecrm.vehicle.list

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*
import java.time.LocalDate

data class ListVehiclesQuery(
    val companyId: CompanyId,
    val status: VehicleStatus?,
    val vehicleType: VehicleType?,
    val pageable: Pageable
)

data class VehicleListItem(
    val id: VehicleId,
    val registrationNumber: String,
    val make: String,
    val model: String,
    val year: Int,
    val vehicleType: VehicleType,
    val status: VehicleStatus,
    val totalSeats: Int,
    val wheelchairSpaces: Int,
    val insuranceValidUntil: LocalDate?,
    val technicalInspectionValidUntil: LocalDate?
)

@Component
class ListVehiclesHandler(
    private val vehicleRepository: VehicleRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListVehiclesQuery): Page<VehicleListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val vehicles = vehicleRepository.findAll(
            companyId = query.companyId,
            status = query.status,
            vehicleType = query.vehicleType,
            pageable = query.pageable
        )

        return vehicles.map { vehicle ->
            VehicleListItem(
                id = vehicle.id,
                registrationNumber = vehicle.registrationNumber,
                make = vehicle.make,
                model = vehicle.model,
                year = vehicle.year,
                vehicleType = vehicle.vehicleType,
                status = vehicle.status,
                totalSeats = vehicle.capacity.totalSeats,
                wheelchairSpaces = vehicle.capacity.wheelchairSpaces,
                insuranceValidUntil = vehicle.insurance?.validUntil,
                technicalInspectionValidUntil = vehicle.technicalInspection?.validUntil
            )
        }
    }
}