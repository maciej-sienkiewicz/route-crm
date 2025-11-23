// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/domain/services/DriverAbsenceRouteSyncService.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.domain.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsence
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesId
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

data class AffectedRouteDetail(
    val id: RouteId,
    val routeName: String,
    val date: LocalDate,
    val previousStatus: RouteStatus,
    val newStatus: RouteStatus
)

data class AffectedSeriesDetail(
    val id: RouteSeriesId,
    val seriesName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?
)

data class RouteSyncResult(
    val routesUpdated: Int,
    val seriesUpdated: Int,
    val affectedRoutes: List<AffectedRouteDetail>,
    val affectedSeries: List<AffectedSeriesDetail>
)

@Component
class DriverAbsenceRouteSyncService(
    private val routeRepository: RouteRepository,
    private val seriesRepository: RouteSeriesRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun syncAbsenceWithRoutes(
        companyId: CompanyId,
        absence: DriverAbsence
    ): RouteSyncResult {
        logger.info(
            "Syncing absence ${absence.id.value} for driver ${absence.driverId.value} " +
                    "from ${absence.startDate} to ${absence.endDate}"
        )

        // 1. Znajdź wszystkie trasy kierowcy w okresie absencji
        val affectedRoutes = findAffectedRoutes(companyId, absence)

        // 2. Zgrupuj trasy po seriach
        val routesBySeries = affectedRoutes.groupBy { it.seriesId }

        // 3. Zaktualizuj serie (jeśli są dotknięte)
        val affectedSeriesDetails = updateAffectedSeries(companyId, routesBySeries, absence.driverId)

        // 4. Zaktualizuj pojedyncze trasy
        val affectedRoutesDetails = updateAffectedRoutes(affectedRoutes)

        val result = RouteSyncResult(
            routesUpdated = affectedRoutesDetails.size,
            seriesUpdated = affectedSeriesDetails.size,
            affectedRoutes = affectedRoutesDetails,
            affectedSeries = affectedSeriesDetails
        )

        logger.info(
            "Sync complete for absence ${absence.id.value}: " +
                    "${result.routesUpdated} routes updated, ${result.seriesUpdated} series updated"
        )

        return result
    }

    private suspend fun findAffectedRoutes(
        companyId: CompanyId,
        absence: DriverAbsence
    ): List<Route> {
        val routes = mutableListOf<Route>()
        var currentDate = absence.startDate

        while (!currentDate.isAfter(absence.endDate)) {
            val dayRoutes = routeRepository.findAll(
                companyId = companyId,
                date = currentDate,
                status = null,
                driverId = absence.driverId,
                pageable = org.springframework.data.domain.Pageable.unpaged()
            ).content.filter { route ->
                // Tylko trasy, które mogą być zaktualizowane
                route.status in setOf(RouteStatus.PLANNED, RouteStatus.DRIVER_MISSING) &&
                        route.driverId == absence.driverId
            }

            routes.addAll(dayRoutes)
            currentDate = currentDate.plusDays(1)
        }

        return routes
    }

    private suspend fun updateAffectedSeries(
        companyId: CompanyId,
        routesBySeries: Map<RouteSeriesId?, List<Route>>,
        driverId: DriverId
    ): List<AffectedSeriesDetail> {
        val affectedSeries = mutableListOf<AffectedSeriesDetail>()

        routesBySeries.forEach { (seriesId, routes) ->
            if (seriesId != null) {
                val series = seriesRepository.findById(companyId, seriesId)

                if (series != null &&
                    series.driverId == driverId &&
                    series.status == RouteSeriesStatus.ACTIVE) {

                    // Zaktualizuj serię - usuń kierowcę
                    val updatedSeries = series.copy(driverId = null)
                    seriesRepository.save(updatedSeries)

                    affectedSeries.add(
                        AffectedSeriesDetail(
                            id = series.id,
                            seriesName = series.seriesName,
                            startDate = series.startDate,
                            endDate = series.endDate
                        )
                    )

                    logger.info(
                        "Series ${seriesId.value} '${series.seriesName}' updated: driver removed due to absence"
                    )
                }
            }
        }

        return affectedSeries
    }

    private suspend fun updateAffectedRoutes(routes: List<Route>): List<AffectedRouteDetail> {
        val affectedRoutes = mutableListOf<AffectedRouteDetail>()

        routes.forEach { route ->
            val previousStatus = route.status

            // Wywołaj metodę domenową do oznaczenia braku kierowcy
            val updatedRoute = route.markDriverMissing()
            routeRepository.save(updatedRoute)

            affectedRoutes.add(
                AffectedRouteDetail(
                    id = route.id,
                    routeName = route.routeName,
                    date = route.date,
                    previousStatus = previousStatus,
                    newStatus = updatedRoute.status
                )
            )

            logger.debug(
                "Route ${route.id.value} '${route.routeName}' on ${route.date}: " +
                        "status changed from ${previousStatus} to ${updatedRoute.status}, driver removed"
            )
        }

        return affectedRoutes
    }
}