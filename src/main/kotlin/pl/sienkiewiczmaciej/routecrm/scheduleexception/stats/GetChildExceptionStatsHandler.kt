package pl.sienkiewiczmaciej.routecrm.scheduleexception.stats

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class GetChildExceptionStatsQuery(
    val companyId: CompanyId,
    val childId: ChildId,
    val year: Int?,
    val month: Int?
)

data class ChildExceptionStats(
    val childId: ChildId,
    val periodFrom: LocalDate,
    val periodTo: LocalDate,
    val totalExceptions: Int
)

@Component
class GetChildExceptionStatsHandler(
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(
        principal: UserPrincipal,
        query: GetChildExceptionStatsQuery
    ): ChildExceptionStats {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val (from, to) = calculatePeriod(query.year, query.month)

        val count = scheduleExceptionRepository.countByChild(
            companyId = query.companyId,
            childId = query.childId,
            from = from,
            to = to
        )

        return ChildExceptionStats(
            childId = query.childId,
            periodFrom = from,
            periodTo = to,
            totalExceptions = count
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