// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/domain/DashboardModels.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.domain

import java.time.LocalDate

enum class ReadinessStatus {
    READY,
    WARNING,
    CRITICAL
}

enum class CheckType {
    ROUTES_DRIVERS,
    ROUTES_VEHICLES,
    CHILDREN_ASSIGNED,
    DRIVER_DOCUMENTS,
    VEHICLES_TECHNICAL
}

enum class CheckStatus {
    OK,
    WARNING,
    ERROR
}

data class ReadinessCheck(
    val type: CheckType,
    val status: CheckStatus,
    val message: String,
    val count: Int? = null,
    val totalCount: Int? = null
)

data class DashboardReadiness(
    val date: LocalDate,
    val status: ReadinessStatus,
    val routesCount: Int,
    val childrenCount: Int,
    val driversCount: Int,
    val checks: List<ReadinessCheck>
)

data class DashboardAlerts(
    val childrenNoRoutes: Int,
    val routesNoDrivers: Int,
    val driverDocuments: Int,
    val vehicleDocuments: Int,
    val routesNoVehicles: Int
)

enum class AlertType {
    CHILDREN_NO_ROUTES,
    ROUTES_NO_DRIVERS,
    DRIVER_DOCUMENTS,
    VEHICLE_DOCUMENTS,
    ROUTES_NO_VEHICLES
}

enum class AlertSeverity {
    HIGH,
    MEDIUM,
    LOW
}

data class AlertItem(
    val id: String,
    val name: String,
    val details: String,
    val date: LocalDate
)

data class Alert(
    val type: AlertType,
    val severity: AlertSeverity,
    val count: Int,
    val items: List<AlertItem>
)

enum class AlertScope {
    TOMORROW,
    THREE_DAYS,
    SEVEN_DAYS,
    THIRTY_DAYS
}

enum class TrendDirection {
    UP,
    DOWN,
    NEUTRAL
}

data class TrendChange(
    val value: Int,
    val percentage: Double,
    val direction: TrendDirection
)

data class WeeklyMetrics(
    val children: Int,
    val routes: Int,
    val cancellations: Int
)

data class DashboardTrends(
    val current: WeeklyMetrics,
    val previous: WeeklyMetrics,
    val changes: Map<String, TrendChange>
)