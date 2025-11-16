// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/infrastructure/DriverAbsenceRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsence
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Repository
class DriverAbsenceRepositoryImpl(
    private val jpaRepository: DriverAbsenceJpaRepository
) : DriverAbsenceRepository {

    override suspend fun save(absence: DriverAbsence): DriverAbsence = withContext(Dispatchers.IO) {
        val entity = DriverAbsenceEntity.fromDomain(absence)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: DriverAbsenceId): DriverAbsence? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        from: LocalDate?,
        to: LocalDate?,
        statuses: Set<DriverAbsenceStatus>?
    ): List<DriverAbsence> = withContext(Dispatchers.IO) {
        jpaRepository.findByDriverAndDateRange(
            companyId.value,
            driverId.value,
            from,
            to,
            statuses
        ).map { it.toDomain() }
    }

    override suspend fun findActiveAbsencesForDriver(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate
    ): List<DriverAbsence> = withContext(Dispatchers.IO) {
        jpaRepository.findActiveForDriverOnDate(
            companyId.value,
            driverId.value,
            date
        ).map { it.toDomain() }
    }

    override suspend fun countByDriver(
        companyId: CompanyId,
        driverId: DriverId,
        from: LocalDate,
        to: LocalDate,
        statuses: Set<DriverAbsenceStatus>?
    ): Int = withContext(Dispatchers.IO) {
        jpaRepository.countByDriverAndDateRange(
            companyId.value,
            driverId.value,
            from,
            to,
            statuses
        )
    }

    override suspend fun delete(companyId: CompanyId, id: DriverAbsenceId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}