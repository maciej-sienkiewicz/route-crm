// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/domain/DriverAbsenceRepository.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface DriverAbsenceRepository {
    suspend fun save(absence: DriverAbsence): DriverAbsence

    suspend fun findById(companyId: CompanyId, id: DriverAbsenceId): DriverAbsence?

    suspend fun findByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        from: LocalDate?,
        to: LocalDate?,
        statuses: Set<DriverAbsenceStatus>?
    ): List<DriverAbsence>

    suspend fun findActiveAbsencesForDriver(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate
    ): List<DriverAbsence>

    suspend fun countByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        from: LocalDate,
        to: LocalDate,
        statuses: Set<DriverAbsenceStatus>?
    ): Int

    suspend fun delete(companyId: CompanyId, id: DriverAbsenceId)
}