package pl.sienkiewiczmaciej.routecrm.vehicle.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface VehicleRepository {
    suspend fun save(vehicle: Vehicle): Vehicle
    suspend fun findById(companyId: CompanyId, id: VehicleId): Vehicle?
    suspend fun findAll(
        companyId: CompanyId,
        status: VehicleStatus?,
        vehicleType: VehicleType?,
        pageable: Pageable
    ): Page<Vehicle>
    suspend fun existsByRegistrationNumber(companyId: CompanyId, registrationNumber: String): Boolean
    suspend fun existsByRegistrationNumberExcludingId(
        companyId: CompanyId,
        registrationNumber: String,
        excludeId: VehicleId
    ): Boolean
    suspend fun delete(companyId: CompanyId, id: VehicleId)
}