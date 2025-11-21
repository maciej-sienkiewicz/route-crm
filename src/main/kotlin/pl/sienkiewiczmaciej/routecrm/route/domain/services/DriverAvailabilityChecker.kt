// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/services/DriverAvailabilityChecker.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

sealed class DriverAvailabilityResult {
    object Available : DriverAvailabilityResult()

    data class Unavailable(
        val reason: String,
        val absenceType: String,
        val absenceStartDate: LocalDate,
        val absenceEndDate: LocalDate
    ) : DriverAvailabilityResult()
}

/**
 * Domain service for checking driver availability on specific dates.
 * Checks against active driver absences.
 */
@Component
class DriverAvailabilityChecker(
    private val absenceRepository: DriverAbsenceRepository
) {
    suspend fun checkDriverAvailability(
        companyId: CompanyId,
        driverId: DriverId,
        date: LocalDate
    ): DriverAvailabilityResult {
        val activeAbsences = absenceRepository.findActiveAbsencesForDriver(
            companyId = companyId,
            driverId = driverId,
            date = date
        )

        return if (activeAbsences.isEmpty()) {
            DriverAvailabilityResult.Available
        } else {
            val absence = activeAbsences.first()
            DriverAvailabilityResult.Unavailable(
                reason = "Driver has ${absence.absenceType.name.lowercase().replace('_', ' ')} from ${absence.startDate} to ${absence.endDate}",
                absenceType = absence.absenceType.name,
                absenceStartDate = absence.startDate,
                absenceEndDate = absence.endDate
            )
        }
    }

    suspend fun checkDriverAvailabilityForDateRange(
        companyId: CompanyId,
        driverId: DriverId,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Pair<LocalDate, DriverAvailabilityResult>> {
        val results = mutableListOf<Pair<LocalDate, DriverAvailabilityResult>>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val result = checkDriverAvailability(companyId, driverId, currentDate)
            results.add(currentDate to result)
            currentDate = currentDate.plusDays(1)
        }

        return results
    }
}