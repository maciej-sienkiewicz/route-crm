// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/alerts/GetDashboardAlertsHandler.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.alerts

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class GetDashboardAlertsQuery(
    val companyId: CompanyId,
    val scope: AlertScope
)

data class DashboardAlertsResult(
    val alerts: List<Alert>,
    val totalCount: Int,
    val scope: AlertScope
)

@Component
class GetDashboardAlertsHandler(
    private val dashboardRepository: DashboardRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDashboardAlertsQuery): DashboardAlertsResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val (startDate, endDate) = calculateDateRange(query.scope)
        val data = dashboardRepository.getAlertsData(query.companyId, startDate, endDate)

        val alerts = buildAlerts(data, startDate, endDate)
        val totalCount = alerts.sumOf { it.count }

        return DashboardAlertsResult(
            alerts = alerts,
            totalCount = totalCount,
            scope = query.scope
        )
    }

    private fun calculateDateRange(scope: AlertScope): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (scope) {
            AlertScope.TOMORROW -> today.plusDays(1) to today.plusDays(1)
            AlertScope.THREE_DAYS -> today to today.plusDays(3)
            AlertScope.SEVEN_DAYS -> today to today.plusDays(7)
            AlertScope.THIRTY_DAYS -> today to today.plusDays(30)
        }
    }

    private fun buildAlerts(data: AlertsData, startDate: LocalDate, endDate: LocalDate): List<Alert> {
        val alerts = mutableListOf<Alert>()

        // CHILDREN_NO_ROUTES
        if (data.childrenNoRoutes.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.CHILDREN_NO_ROUTES,
                    severity = calculateSeverity(startDate),
                    count = data.childrenNoRoutes.size,
                    items = data.childrenNoRoutes.map { child ->
                        AlertItem(
                            id = child.childId,
                            name = "${child.firstName} ${child.lastName}",
                            details = "No route assigned for ${child.date}",
                            date = child.date
                        )
                    }
                )
            )
        }

        // ROUTES_NO_DRIVERS
        if (data.routesNoDrivers.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.ROUTES_NO_DRIVERS,
                    severity = calculateSeverity(data.routesNoDrivers.minOf { it.date }),
                    count = data.routesNoDrivers.size,
                    items = data.routesNoDrivers.map { route ->
                        AlertItem(
                            id = route.routeId,
                            name = route.routeName,
                            details = "No driver assigned for ${route.date}",
                            date = route.date
                        )
                    }
                )
            )
        }

        // DRIVER_DOCUMENTS
        if (data.driversExpiringDocs.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.DRIVER_DOCUMENTS,
                    severity = calculateSeverity(data.driversExpiringDocs.minOf { it.expiryDate }),
                    count = data.driversExpiringDocs.size,
                    items = data.driversExpiringDocs.map { driver ->
                        AlertItem(
                            id = driver.driverId,
                            name = "${driver.firstName} ${driver.lastName}",
                            details = "${driver.docType} expires on ${driver.expiryDate}",
                            date = driver.expiryDate
                        )
                    }
                )
            )
        }

        // VEHICLE_DOCUMENTS
        if (data.vehiclesExpiringDocs.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.VEHICLE_DOCUMENTS,
                    severity = calculateSeverity(data.vehiclesExpiringDocs.minOf { it.expiryDate }),
                    count = data.vehiclesExpiringDocs.size,
                    items = data.vehiclesExpiringDocs.map { vehicle ->
                        AlertItem(
                            id = vehicle.vehicleId,
                            name = vehicle.registrationNumber,
                            details = "${vehicle.docType} expires on ${vehicle.expiryDate}",
                            date = vehicle.expiryDate
                        )
                    }
                )
            )
        }

        // ROUTES_NO_VEHICLES
        if (data.routesNoVehicles.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.ROUTES_NO_VEHICLES,
                    severity = calculateSeverity(data.routesNoVehicles.minOf { it.date }),
                    count = data.routesNoVehicles.size,
                    items = data.routesNoVehicles.map { route ->
                        AlertItem(
                            id = route.routeId,
                            name = route.routeName,
                            details = "No vehicle assigned for ${route.date}",
                            date = route.date
                        )
                    }
                )
            )
        }

        return alerts
    }

    private fun calculateSeverity(date: LocalDate): AlertSeverity {
        val daysUntil = LocalDate.now().until(date, java.time.temporal.ChronoUnit.DAYS)
        return when {
            daysUntil <= 1 -> AlertSeverity.HIGH
            daysUntil <= 7 -> AlertSeverity.MEDIUM
            else -> AlertSeverity.LOW
        }
    }
}