package pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*

@Repository
class VehicleRepositoryImpl(
    private val jpaRepository: VehicleJpaRepository
) : VehicleRepository {

    override suspend fun save(vehicle: Vehicle): Vehicle = withContext(Dispatchers.IO) {
        val entity = VehicleEntity.fromDomain(vehicle)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: VehicleId): Vehicle? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findAll(
        companyId: CompanyId,
        status: VehicleStatus?,
        vehicleType: VehicleType?,
        pageable: Pageable
    ): Page<Vehicle> = withContext(Dispatchers.IO) {
        val page = when {
            status != null && vehicleType != null -> {
                jpaRepository.findByCompanyIdAndStatusAndVehicleType(
                    companyId.value,
                    status,
                    vehicleType,
                    pageable
                )
            }
            status != null -> {
                jpaRepository.findByCompanyIdAndStatus(companyId.value, status, pageable)
            }
            vehicleType != null -> {
                jpaRepository.findByCompanyIdAndVehicleType(companyId.value, vehicleType, pageable)
            }
            else -> {
                jpaRepository.findByCompanyId(companyId.value, pageable)
            }
        }
        page.map { it.toDomain() }
    }

    override suspend fun existsByRegistrationNumber(
        companyId: CompanyId,
        registrationNumber: String
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndRegistrationNumber(
            companyId.value,
            registrationNumber.uppercase()
        )
    }

    override suspend fun existsByRegistrationNumberExcludingId(
        companyId: CompanyId,
        registrationNumber: String,
        excludeId: VehicleId
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndRegistrationNumberExcludingId(
            companyId.value,
            registrationNumber.uppercase(),
            excludeId.value
        )
    }

    override suspend fun delete(companyId: CompanyId, id: VehicleId) {
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.let {
                jpaRepository.delete(it)
            }
        }
    }
}