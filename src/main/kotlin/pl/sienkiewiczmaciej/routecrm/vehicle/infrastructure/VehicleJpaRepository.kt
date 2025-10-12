package pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleStatus
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleType

interface VehicleJpaRepository : JpaRepository<VehicleEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): VehicleEntity?

    fun findByCompanyId(companyId: String, pageable: Pageable): Page<VehicleEntity>

    fun findByCompanyIdAndStatus(
        companyId: String,
        status: VehicleStatus,
        pageable: Pageable
    ): Page<VehicleEntity>

    fun findByCompanyIdAndVehicleType(
        companyId: String,
        vehicleType: VehicleType,
        pageable: Pageable
    ): Page<VehicleEntity>

    fun findByCompanyIdAndStatusAndVehicleType(
        companyId: String,
        status: VehicleStatus,
        vehicleType: VehicleType,
        pageable: Pageable
    ): Page<VehicleEntity>

    fun existsByCompanyIdAndRegistrationNumber(
        companyId: String,
        registrationNumber: String
    ): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
        FROM VehicleEntity v
        WHERE v.companyId = :companyId
        AND v.registrationNumber = :registrationNumber
        AND v.id != :excludeId
    """)
    fun existsByCompanyIdAndRegistrationNumberExcludingId(
        companyId: String,
        registrationNumber: String,
        excludeId: String
    ): Boolean
}