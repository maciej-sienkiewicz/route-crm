// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/domain/services/AbsenceConflictChecker.kt
package pl.sienkiewiczmaciej.routecrm.absence.domain.services

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceConflict
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Component
class AbsenceConflictChecker(
    private val absenceRepository: ChildAbsenceRepository
) {
    suspend fun checkConflictsForSchedule(
        companyId: CompanyId,
        childId: ChildId,
        scheduleId: ScheduleId,
        date: LocalDate
    ): List<AbsenceConflict> {
        val absences = absenceRepository.findActiveAbsencesForChild(companyId, childId, date)

        return absences.mapNotNull { absence ->
            when {
                absence.coversSchedule(scheduleId, date) -> {
                    val type = when (absence.absenceType) {
                        pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType.FULL_DAY ->
                            AbsenceConflict.ConflictType.FULL_DAY_ABSENCE
                        pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType.SPECIFIC_SCHEDULE ->
                            AbsenceConflict.ConflictType.SCHEDULE_SPECIFIC_ABSENCE
                    }
                    AbsenceConflict(absence, type)
                }
                else -> null
            }
        }
    }

    suspend fun hasActiveAbsence(
        companyId: CompanyId,
        childId: ChildId,
        date: LocalDate
    ): Boolean {
        val absences = absenceRepository.findActiveAbsencesForChild(companyId, childId, date)
        return absences.isNotEmpty()
    }

    suspend fun hasActiveAbsenceForSchedule(
        companyId: CompanyId,
        childId: ChildId,
        scheduleId: ScheduleId,
        date: LocalDate
    ): Boolean {
        val absences = absenceRepository.findActiveAbsencesForSchedule(
            companyId, childId, scheduleId, date
        )
        return absences.isNotEmpty()
    }
}