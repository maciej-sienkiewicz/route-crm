// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/infrastructure/ChildAbsenceRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.absence.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsence
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Repository
class ChildAbsenceRepositoryImpl(
    private val jpaRepository: ChildAbsenceJpaRepository
) : ChildAbsenceRepository {

    override suspend fun save(absence: ChildAbsence): ChildAbsence = withContext(Dispatchers.IO) {
        val entity = ChildAbsenceEntity.fromDomain(absence)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: ChildAbsenceId): ChildAbsence? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate?,
        to: LocalDate?,
        statuses: Set<AbsenceStatus>?
    ): List<ChildAbsence> = withContext(Dispatchers.IO) {
        jpaRepository.findByChildAndDateRange(
            companyId.value,
            childId.value,
            from,
            to,
            statuses
        ).map { it.toDomain() }
    }

    override suspend fun findBySchedule(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        from: LocalDate?,
        to: LocalDate?
    ): List<ChildAbsence> = withContext(Dispatchers.IO) {
        jpaRepository.findByScheduleAndDateRange(
            companyId.value,
            scheduleId.value,
            from,
            to
        ).map { it.toDomain() }
    }

    override suspend fun findActiveAbsencesForChild(
        companyId: CompanyId,
        childId: ChildId,
        date: LocalDate
    ): List<ChildAbsence> = withContext(Dispatchers.IO) {
        jpaRepository.findActiveForChildOnDate(
            companyId.value,
            childId.value,
            date
        ).map { it.toDomain() }
    }

    override suspend fun findActiveAbsencesForSchedule(
        companyId: CompanyId,
        childId: ChildId,
        scheduleId: ScheduleId,
        date: LocalDate
    ): List<ChildAbsence> = withContext(Dispatchers.IO) {
        jpaRepository.findActiveForScheduleOnDate(
            companyId.value,
            childId.value,
            scheduleId.value,
            date
        ).map { it.toDomain() }
    }

    override suspend fun countByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate,
        to: LocalDate,
        statuses: Set<AbsenceStatus>?
    ): Int = withContext(Dispatchers.IO) {
        jpaRepository.countByChildAndDateRange(
            companyId.value,
            childId.value,
            from,
            to,
            statuses
        )
    }

    override suspend fun delete(companyId: CompanyId, id: ChildAbsenceId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}