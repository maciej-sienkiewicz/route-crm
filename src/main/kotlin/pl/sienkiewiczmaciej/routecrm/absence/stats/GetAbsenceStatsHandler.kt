// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/stats/GetAbsenceStatsHandler.kt
package pl.sienkiewiczmaciej.routecrm.absence.stats

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceStatus
import pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class GetAbsenceStatsQuery(
    val companyId: CompanyId,
    val childId: ChildId,
    val year: Int?,
    val month: Int?
)

data class AbsenceStats(
    val childId: ChildId,
    val periodFrom: LocalDate,
    val periodTo: LocalDate,
    val totalAbsences: Int,
    val totalDays: Int,
    val byType: Map<AbsenceType, Int>,
    val byStatus: Map<AbsenceStatus, Int>
)

@Component
class GetAbsenceStatsHandler(
    private val absenceRepository: ChildAbsenceRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetAbsenceStatsQuery): AbsenceStats {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        if (principal.role == UserRole.GUARDIAN) {
            require(principal.guardianId != null) { "Guardian must have guardianId" }
            // TODO: Verify guardian owns this child
        }

        val (from, to) = calculatePeriod(query.year, query.month)

        val absences = absenceRepository.findByChild(
            companyId = query.companyId,
            childId = query.childId,
            from = from,
            to = to,
            statuses = null
        )

        val totalDays = absences.sumOf { absence ->
            val start = maxOf(absence.startDate, from)
            val end = minOf(absence.endDate, to)
            java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
        }

        val byType = absences.groupingBy { it.absenceType }.eachCount()
        val byStatus = absences.groupingBy { it.getCurrentStatus() }.eachCount()

        return AbsenceStats(
            childId = query.childId,
            periodFrom = from,
            periodTo = to,
            totalAbsences = absences.size,
            totalDays = totalDays,
            byType = byType,
            byStatus = byStatus
        )
    }

    private fun calculatePeriod(year: Int?, month: Int?): Pair<LocalDate, LocalDate> {
        val currentYear = year ?: LocalDate.now().year

        return if (month != null) {
            val from = LocalDate.of(currentYear, month, 1)
            val to = from.plusMonths(1).minusDays(1)
            Pair(from, to)
        } else {
            val from = LocalDate.of(currentYear, 1, 1)
            val to = LocalDate.of(currentYear, 12, 31)
            Pair(from, to)
        }
    }
}