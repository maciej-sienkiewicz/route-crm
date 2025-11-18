// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/summary/GetDashboardSummaryHandler.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.summary

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class GetDashboardSummaryQuery(
    val companyId: CompanyId,
    val date: LocalDate
)

data class DashboardSummaryResult(
    val date: LocalDate,
    val readiness: DashboardReadiness,
    val alerts: DashboardAlerts
)

@Component
class GetDashboardSummaryHandler(
    private val dashboardRepository: DashboardRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDashboardSummaryQuery): DashboardSummaryResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        require(!query.date.isBefore(LocalDate.now())) {
            "Cannot check readiness for past dates"
        }

        require(!query.date.isAfter(LocalDate.now().plusDays(90))) {
            "Readiness can be checked up to 90 days in advance"
        }

        val data = dashboardRepository.getReadinessData(query.companyId, query.date)

        val checks = buildChecks(data, query.date)
        val status = calculateGlobalStatus(data)

        val readiness = DashboardReadiness(
            date = query.date,
            status = status,
            routesCount = data.totalRoutes,
            childrenCount = data.totalActiveChildren,
            driversCount = data.uniqueDriversCount,
            checks = checks
        )

        val alerts = DashboardAlerts(
            childrenNoRoutes = data.childrenWithoutRoutes,
            routesNoDrivers = data.routesWithoutDrivers,
            driverDocuments = data.driversWithExpiringDocs,
            vehicleDocuments = data.vehiclesWithExpiringDocs,
            routesNoVehicles = data.routesWithoutVehicles
        )

        return DashboardSummaryResult(
            date = query.date,
            readiness = readiness,
            alerts = alerts
        )
    }

    private fun buildChecks(data: ReadinessData, date: LocalDate): List<ReadinessCheck> {
        val checks = mutableListOf<ReadinessCheck>()

        // ROUTES_DRIVERS
        checks.add(
            ReadinessCheck(
                type = CheckType.ROUTES_DRIVERS,
                status = if (data.routesWithoutDrivers > 0) CheckStatus.ERROR else CheckStatus.OK,
                message = if (data.routesWithoutDrivers > 0) {
                    "${data.routesWithoutDrivers} routes without drivers"
                } else {
                    "All routes have drivers"
                },
                count = data.totalRoutes - data.routesWithoutDrivers,
                totalCount = data.totalRoutes
            )
        )

        // ROUTES_VEHICLES
        checks.add(
            ReadinessCheck(
                type = CheckType.ROUTES_VEHICLES,
                status = if (data.routesWithoutVehicles > 0) CheckStatus.ERROR else CheckStatus.OK,
                message = if (data.routesWithoutVehicles > 0) {
                    "${data.routesWithoutVehicles} routes without vehicles"
                } else {
                    "All routes have vehicles"
                },
                count = data.totalRoutes - data.routesWithoutVehicles,
                totalCount = data.totalRoutes
            )
        )

        // CHILDREN_ASSIGNED
        checks.add(
            ReadinessCheck(
                type = CheckType.CHILDREN_ASSIGNED,
                status = if (data.childrenWithoutRoutes > 0) CheckStatus.ERROR else CheckStatus.OK,
                message = if (data.childrenWithoutRoutes > 0) {
                    "${data.childrenWithoutRoutes} children without routes"
                } else {
                    "All children assigned"
                },
                count = data.totalActiveChildren - data.childrenWithoutRoutes,
                totalCount = data.totalActiveChildren
            )
        )

        // DRIVER_DOCUMENTS
        checks.add(
            ReadinessCheck(
                type = CheckType.DRIVER_DOCUMENTS,
                status = if (data.driversWithExpiringDocs > 0) CheckStatus.WARNING else CheckStatus.OK,
                message = if (data.driversWithExpiringDocs > 0) {
                    "${data.driversWithExpiringDocs} drivers with documents expiring within 30 days"
                } else {
                    "All driver documents valid"
                },
                count = data.driversWithExpiringDocs
            )
        )

        // VEHICLES_TECHNICAL
        checks.add(
            ReadinessCheck(
                type = CheckType.VEHICLES_TECHNICAL,
                status = if (data.vehiclesWithExpiringDocs > 0) CheckStatus.WARNING else CheckStatus.OK,
                message = if (data.vehiclesWithExpiringDocs > 0) {
                    "${data.vehiclesWithExpiringDocs} vehicles with documents expiring within 30 days"
                } else {
                    "All vehicles technically sound"
                },
                count = data.vehiclesWithExpiringDocs
            )
        )

        return checks
    }

    private fun calculateGlobalStatus(data: ReadinessData): ReadinessStatus {
        return when {
            data.routesWithoutDrivers > 0 ||
                    data.routesWithoutVehicles > 0 ||
                    data.childrenWithoutRoutes > 0 -> ReadinessStatus.CRITICAL

            data.driversWithExpiringDocs > 0 ||
                    data.vehiclesWithExpiringDocs > 0 -> ReadinessStatus.WARNING

            else -> ReadinessStatus.READY
        }
    }
}