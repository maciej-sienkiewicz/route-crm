// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/domain/DashboardRepository.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface DashboardRepository {
    suspend fun getReadinessData(companyId: CompanyId, date: LocalDate): ReadinessData
    suspend fun getAlertsData(companyId: CompanyId, startDate: LocalDate, endDate: LocalDate): AlertsData
    suspend fun getTrendsData(companyId: CompanyId, currentWeekStart: LocalDate, currentWeekEnd: LocalDate, previousWeekStart: LocalDate, previousWeekEnd: LocalDate): TrendsData
}

data class ReadinessData(
    val routesWithoutDrivers: Int,
    val totalRoutes: Int,
    val routesWithoutVehicles: Int,
    val childrenWithoutRoutes: Int,
    val totalActiveChildren: Int,
    val driversWithExpiringDocs: Int,
    val vehiclesWithExpiringDocs: Int,
    val uniqueDriversCount: Int
)

data class AlertsData(
    val childrenNoRoutes: List<ChildAlertItem>,
    val routesNoDrivers: List<RouteAlertItem>,
    val driversExpiringDocs: List<DriverDocAlertItem>,
    val vehiclesExpiringDocs: List<VehicleDocAlertItem>,
    val routesNoVehicles: List<RouteAlertItem>
)

data class ChildAlertItem(
    val childId: String,
    val firstName: String,
    val lastName: String,
    val date: LocalDate
)

data class RouteAlertItem(
    val routeId: String,
    val routeName: String,
    val date: LocalDate
)

data class DriverDocAlertItem(
    val driverId: String,
    val firstName: String,
    val lastName: String,
    val docType: String,
    val expiryDate: LocalDate
)

data class VehicleDocAlertItem(
    val vehicleId: String,
    val registrationNumber: String,
    val docType: String,
    val expiryDate: LocalDate
)

data class TrendsData(
    val currentChildren: Int,
    val previousChildren: Int,
    val currentRoutes: Int,
    val previousRoutes: Int,
    val currentCancellations: Int,
    val previousCancellations: Int
)