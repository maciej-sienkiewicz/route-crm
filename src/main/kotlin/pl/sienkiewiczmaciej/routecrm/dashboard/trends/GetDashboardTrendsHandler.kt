// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/trends/GetDashboardTrendsHandler.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.trends

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs

data class GetDashboardTrendsQuery(
    val companyId: CompanyId
)

@Component
class GetDashboardTrendsHandler(
    private val dashboardRepository: DashboardRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDashboardTrendsQuery): DashboardTrends {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val (currentWeekStart, currentWeekEnd) = getCurrentWeekRange()
        val (previousWeekStart, previousWeekEnd) = getPreviousWeekRange()

        val data = dashboardRepository.getTrendsData(
            query.companyId,
            currentWeekStart,
            currentWeekEnd,
            previousWeekStart,
            previousWeekEnd
        )

        val current = WeeklyMetrics(
            children = data.currentChildren,
            routes = data.currentRoutes,
            cancellations = data.currentCancellations
        )

        val previous = WeeklyMetrics(
            children = data.previousChildren,
            routes = data.previousRoutes,
            cancellations = data.previousCancellations
        )

        val changes = mapOf(
            "children" to calculateChange(data.currentChildren, data.previousChildren),
            "routes" to calculateChange(data.currentRoutes, data.previousRoutes),
            "cancellations" to calculateChange(data.currentCancellations, data.previousCancellations)
        )

        return DashboardTrends(
            current = current,
            previous = previous,
            changes = changes
        )
    }

    private fun getCurrentWeekRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val sunday = monday.plusDays(6)
        return monday to sunday
    }

    private fun getPreviousWeekRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY).minusWeeks(1)
        val sunday = monday.plusDays(6)
        return monday to sunday
    }

    private fun calculateChange(current: Int, previous: Int): TrendChange {
        val value = current - previous
        val percentage = if (previous > 0) {
            (value.toDouble() / previous.toDouble()) * 100.0
        } else {
            if (current > 0) 100.0 else 0.0
        }

        val direction = when {
            value > 0 -> TrendDirection.UP
            value < 0 -> TrendDirection.DOWN
            else -> TrendDirection.NEUTRAL
        }

        return TrendChange(
            value = value,
            percentage = String.format("%.1f", abs(percentage)).toDouble(),
            direction = direction
        )
    }
}