// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/infrastructure/DashboardRepositoryImpl.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Repository
class DashboardRepositoryImpl(
    private val dashboardJpaRepository: DashboardJpaRepository
) : DashboardRepository {

    override suspend fun getReadinessData(companyId: CompanyId, date: LocalDate): ReadinessData =
        withContext(Dispatchers.IO) {
            val routesWithoutDrivers = dashboardJpaRepository.countRoutesWithoutDrivers(companyId.value, date)
            val totalRoutes = dashboardJpaRepository.countTotalRoutes(companyId.value, date)
            val routesWithoutVehicles = dashboardJpaRepository.countRoutesWithoutVehicles(companyId.value, date)
            val childrenWithoutRoutes = dashboardJpaRepository.countChildrenWithoutRoutes(companyId.value, date)
            val totalActiveChildren = dashboardJpaRepository.countTotalActiveChildren(companyId.value, date)
            val driversWithExpiringDocs = dashboardJpaRepository.countDriversWithExpiringDocs(companyId.value, date)
            val vehiclesWithExpiringDocs = dashboardJpaRepository.countVehiclesWithExpiringDocs(companyId.value, date)
            val uniqueDriversCount = dashboardJpaRepository.countUniqueDrivers(companyId.value, date)

            ReadinessData(
                routesWithoutDrivers = routesWithoutDrivers,
                totalRoutes = totalRoutes,
                routesWithoutVehicles = routesWithoutVehicles,
                childrenWithoutRoutes = childrenWithoutRoutes,
                totalActiveChildren = totalActiveChildren,
                driversWithExpiringDocs = driversWithExpiringDocs,
                vehiclesWithExpiringDocs = vehiclesWithExpiringDocs,
                uniqueDriversCount = uniqueDriversCount
            )
        }

    override suspend fun getAlertsData(
        companyId: CompanyId,
        startDate: LocalDate,
        endDate: LocalDate
    ): AlertsData = withContext(Dispatchers.IO) {
        // For children without routes, we need to check each date in the range
        val childrenNoRoutes = mutableListOf<ChildAlertItem>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dailyChildren = dashboardJpaRepository.findChildrenWithoutRoutesSingleDate(
                companyId.value,
                currentDate
            )
            childrenNoRoutes.addAll(dailyChildren)
            currentDate = currentDate.plusDays(1)
        }

        val routesNoDrivers = dashboardJpaRepository.findRoutesWithoutDrivers(companyId.value, startDate, endDate)
        val driversExpiringDocs = dashboardJpaRepository.findDriversWithExpiringDocs(companyId.value, startDate, endDate)
        val vehiclesExpiringDocs = dashboardJpaRepository.findVehiclesWithExpiringDocs(companyId.value, startDate, endDate)
        val routesNoVehicles = dashboardJpaRepository.findRoutesWithoutVehicles(companyId.value, startDate, endDate)

        AlertsData(
            childrenNoRoutes = childrenNoRoutes.distinctBy { it.childId to it.date },
            routesNoDrivers = routesNoDrivers,
            driversExpiringDocs = driversExpiringDocs,
            vehiclesExpiringDocs = vehiclesExpiringDocs,
            routesNoVehicles = routesNoVehicles
        )
    }

    override suspend fun getTrendsData(
        companyId: CompanyId,
        currentWeekStart: LocalDate,
        currentWeekEnd: LocalDate,
        previousWeekStart: LocalDate,
        previousWeekEnd: LocalDate
    ): TrendsData = withContext(Dispatchers.IO) {
        val currentChildren = dashboardJpaRepository.countUniqueChildrenInWeek(companyId.value, currentWeekStart, currentWeekEnd)
        val previousChildren = dashboardJpaRepository.countUniqueChildrenInWeek(companyId.value, previousWeekStart, previousWeekEnd)
        val currentRoutes = dashboardJpaRepository.countRoutesInWeek(companyId.value, currentWeekStart, currentWeekEnd)
        val previousRoutes = dashboardJpaRepository.countRoutesInWeek(companyId.value, previousWeekStart, previousWeekEnd)
        val currentCancellations = dashboardJpaRepository.countCancellationsInWeek(companyId.value, currentWeekStart, currentWeekEnd)
        val previousCancellations = dashboardJpaRepository.countCancellationsInWeek(companyId.value, previousWeekStart, previousWeekEnd)

        TrendsData(
            currentChildren = currentChildren,
            previousChildren = previousChildren,
            currentRoutes = currentRoutes,
            previousRoutes = previousRoutes,
            currentCancellations = currentCancellations,
            previousCancellations = previousCancellations
        )
    }
}