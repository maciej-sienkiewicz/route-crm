// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/domain/ChildAbsenceRepository.kt
package pl.sienkiewiczmaciej.routecrm.absence.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface ChildAbsenceRepository {
    suspend fun save(absence: ChildAbsence): ChildAbsence

    suspend fun findById(companyId: CompanyId, id: ChildAbsenceId): ChildAbsence?

    suspend fun findByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate?,
        to: LocalDate?,
        statuses: Set<AbsenceStatus>?
    ): List<ChildAbsence>

    suspend fun findBySchedule(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        from: LocalDate?,
        to: LocalDate?
    ): List<ChildAbsence>

    suspend fun findActiveAbsencesForChild(
        companyId: CompanyId,
        childId: ChildId,
        date: LocalDate
    ): List<ChildAbsence>

    suspend fun findActiveAbsencesForSchedule(
        companyId: CompanyId,
        childId: ChildId,
        scheduleId: ScheduleId,
        date: LocalDate
    ): List<ChildAbsence>

    suspend fun countByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate,
        to: LocalDate,
        statuses: Set<AbsenceStatus>?
    ): Int

    suspend fun delete(companyId: CompanyId, id: ChildAbsenceId)
}