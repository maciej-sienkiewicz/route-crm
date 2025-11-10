// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/upcoming/GetUpcomingRoutesContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.route.upcoming

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.ScheduleNotFoundException

data class GetUpcomingRoutesValidationContext(
    val schedule: Schedule,
    val routes: Page<Route>
)

@Component
class GetUpcomingRoutesContextBuilder(
    private val scheduleRepository: ScheduleRepository,
    private val routeRepository: RouteRepository
) {
    suspend fun build(query: GetUpcomingRoutesQuery): GetUpcomingRoutesValidationContext = coroutineScope {
        // Load schedule to validate it exists
        val scheduleDeferred = async {
            scheduleRepository.findById(query.companyId, query.scheduleId)
                ?: throw ScheduleNotFoundException(query.scheduleId)
        }

        // Load upcoming routes
        val routesDeferred = async {
            routeRepository.findBySchedule(
                companyId = query.companyId,
                scheduleId = query.scheduleId,
                statuses = setOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS),
                pageable = query.pageable
            )
        }

        GetUpcomingRoutesValidationContext(
            schedule = scheduleDeferred.await(),
            routes = routesDeferred.await()
        )
    }
}