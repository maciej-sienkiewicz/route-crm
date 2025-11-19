// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/DashboardController.kt
package pl.sienkiewiczmaciej.routecrm.dashboard

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.sienkiewiczmaciej.routecrm.dashboard.alerts.GetDashboardAlertsHandler
import pl.sienkiewiczmaciej.routecrm.dashboard.alerts.GetDashboardAlertsQuery
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.*
import pl.sienkiewiczmaciej.routecrm.dashboard.summary.GetDashboardSummaryHandler
import pl.sienkiewiczmaciej.routecrm.dashboard.summary.GetDashboardSummaryQuery
import pl.sienkiewiczmaciej.routecrm.dashboard.trends.GetDashboardTrendsHandler
import pl.sienkiewiczmaciej.routecrm.dashboard.trends.GetDashboardTrendsQuery
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.LocalDate

// ============================================
// RESPONSE DTOs
// ============================================

data class ReadinessCheckResponse(
    val type: CheckType,
    val status: CheckStatus,
    val message: String,
    val count: Int?,
    val totalCount: Int?
)

data class DashboardReadinessResponse(
    val status: ReadinessStatus,
    val routesCount: Int,
    val childrenCount: Int,
    val driversCount: Int,
    val checks: List<ReadinessCheckResponse>
)

data class DashboardAlertsResponse(
    val childrenNoRoutes: Int,
    val routesNoDrivers: Int,
    val driverDocuments: Int,
    val vehicleDocuments: Int,
    val routesNoVehicles: Int
)

data class DashboardSummaryResponse(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val date: LocalDate,
    val readiness: DashboardReadinessResponse,
    val alerts: DashboardAlertsResponse
)

data class AlertItemResponse(
    val id: String,
    val name: String,
    val details: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val date: LocalDate
)

data class AlertResponse(
    val type: AlertType,
    val severity: AlertSeverity,
    val count: Int,
    val items: List<AlertItemResponse>
)

data class DashboardAlertsListResponse(
    val alerts: List<AlertResponse>,
    val totalCount: Int,
    val scope: AlertScope
)

data class TrendChangeResponse(
    val value: Int,
    val percentage: Double,
    val direction: TrendDirection
)

data class WeeklyMetricsResponse(
    val children: Int,
    val routes: Int,
    val cancellations: Int
)

data class DashboardTrendsResponse(
    val current: WeeklyMetricsResponse,
    val previous: WeeklyMetricsResponse,
    val changes: Map<String, TrendChangeResponse>
)

// ============================================
// CONTROLLER
// ============================================

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val summaryHandler: GetDashboardSummaryHandler,
    private val alertsHandler: GetDashboardAlertsHandler,
    private val trendsHandler: GetDashboardTrendsHandler,
    private val scheduleRepository: ScheduleRepository,
) : BaseController() {

    @GetMapping("/summary")
    suspend fun getSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): DashboardSummaryResponse {
        val principal = getPrincipal()
        val query = GetDashboardSummaryQuery(
            companyId = principal.companyId,
            date = date
        )

        val result = summaryHandler.handle(principal, query)

        return DashboardSummaryResponse(
            date = result.date,
            readiness = DashboardReadinessResponse(
                status = result.readiness.status,
                routesCount = result.readiness.routesCount,
                childrenCount = result.readiness.childrenCount,
                driversCount = result.readiness.driversCount,
                checks = result.readiness.checks.map { check ->
                    ReadinessCheckResponse(
                        type = check.type,
                        status = check.status,
                        message = check.message,
                        count = check.count,
                        totalCount = check.totalCount
                    )
                }
            ),
            alerts = DashboardAlertsResponse(
                childrenNoRoutes = result.alerts.childrenNoRoutes,
                routesNoDrivers = result.alerts.routesNoDrivers,
                driverDocuments = result.alerts.driverDocuments,
                vehicleDocuments = result.alerts.vehicleDocuments,
                routesNoVehicles = result.alerts.routesNoVehicles
            )
        )
    }

    @GetMapping("/alerts")
    suspend fun getAlerts(
        @RequestParam scope: AlertScope
    ): DashboardAlertsListResponse {
        val principal = getPrincipal()
        val query = GetDashboardAlertsQuery(
            companyId = principal.companyId,
            scope = scope
        )

        val result = alertsHandler.handle(principal, query)

        return DashboardAlertsListResponse(
            alerts = result.alerts.map { alert ->
                AlertResponse(
                    type = alert.type,
                    severity = alert.severity,
                    count = alert.count,
                    items = alert.items.map { item ->
                        AlertItemResponse(
                            id = item.id,
                            name = item.name,
                            details = item.details,
                            date = item.date
                        )
                    }
                )
            },
            totalCount = result.totalCount,
            scope = result.scope
        )
    }

    @GetMapping("/trends")
    suspend fun getTrends(): DashboardTrendsResponse {
        val principal = getPrincipal()
        val query = GetDashboardTrendsQuery(
            companyId = principal.companyId
        )

        val result = trendsHandler.handle(principal, query)

        return DashboardTrendsResponse(
            current = WeeklyMetricsResponse(
                children = result.current.children,
                routes = result.current.routes,
                cancellations = result.current.cancellations
            ),
            previous = WeeklyMetricsResponse(
                children = result.previous.children,
                routes = result.previous.routes,
                cancellations = result.previous.cancellations
            ),
            changes = result.changes.mapValues { (_, change) ->
                TrendChangeResponse(
                    value = change.value,
                    percentage = change.percentage,
                    direction = change.direction
                )
            }
        )
    }
}