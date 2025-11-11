// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/addschedule/AddScheduleAbsenceValidator.kt
package pl.sienkiewiczmaciej.routecrm.route.addschedule

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.absence.domain.services.AbsenceConflictChecker

@Component
class AddScheduleAbsenceValidator(
    private val absenceConflictChecker: AbsenceConflictChecker
) {
    suspend fun validate(command: AddRouteScheduleCommand, context: AddRouteScheduleValidationContext) {
        val conflicts = absenceConflictChecker.checkConflictsForSchedule(
            companyId = command.companyId,
            childId = command.childId,
            scheduleId = command.scheduleId,
            date = context.route.date
        )

        if (conflicts.isNotEmpty()) {
            val conflict = conflicts.first()
            val message = when (conflict.conflictType) {
                pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceConflict.ConflictType.FULL_DAY_ABSENCE ->
                    "Child ${command.childId.value} has a full-day absence on ${context.route.date}. " +
                            "Reason: ${conflict.absence.reason ?: "Not specified"}"

                pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceConflict.ConflictType.SCHEDULE_SPECIFIC_ABSENCE ->
                    "Child ${command.childId.value} has an absence for schedule ${command.scheduleId.value} on ${context.route.date}. " +
                            "Reason: ${conflict.absence.reason ?: "Not specified"}"
            }
            throw IllegalArgumentException(message)
        }
    }
}