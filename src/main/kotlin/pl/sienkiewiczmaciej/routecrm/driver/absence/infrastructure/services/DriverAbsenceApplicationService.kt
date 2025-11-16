// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/infrastructure/services/DriverAbsenceApplicationService.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.infrastructure.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsence
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository

@Service
class DriverAbsenceApplicationService(
    private val routeJpaRepository: RouteJpaRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun findConflictingRoutes(absence: DriverAbsence): List<Route> {
        logger.info("Finding routes conflicting with absence ${absence.id.value}")

        return withContext(Dispatchers.IO) {
            val routes = mutableListOf<Route>()
            var currentDate = absence.startDate

            while (!currentDate.isAfter(absence.endDate)) {
                val dayRoutes = routeJpaRepository.findByFilters(
                    companyId = absence.companyId.value,
                    date = currentDate,
                    status = null,
                    driverId = absence.driverId.value,
                    pageable = org.springframework.data.domain.Pageable.unpaged()
                ).content.filter {
                    it.status in setOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS)
                }

                routes.addAll(dayRoutes.map { it.toDomain() })
                currentDate = currentDate.plusDays(1)
            }

            logger.info("Found ${routes.size} conflicting routes for absence ${absence.id.value}")
            routes
        }
    }
}