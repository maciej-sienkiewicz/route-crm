// routeseries/addchild/AddChildValidationContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository

@Component
class AddChildValidationContextBuilder(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val seriesScheduleRepository: RouteSeriesScheduleRepository,
    private val scheduleRepository: ScheduleRepository,
    private val childRepository: ChildRepository,
    private val routeRepository: RouteRepository
) {
    suspend fun build(command: AddChildToRouteSeriesCommand): AddChildValidationContext = coroutineScope {
        val seriesDeferred = async {
            routeSeriesRepository.findById(command.companyId, command.seriesId)
                ?: throw RouteSeriesNotFoundException(command.seriesId)
        }

        val scheduleDeferred = async {
            scheduleRepository.findById(command.companyId, command.scheduleId)
                ?: throw IllegalArgumentException("Schedule ${command.scheduleId.value} not found")
        }

        val childDeferred = async {
            childRepository.findById(command.companyId, command.childId)
                ?: throw ChildNotFoundException(command.childId)
        }

        val existingScheduleDeferred = async {
            seriesScheduleRepository.findBySeriesAndSchedule(
                companyId = command.companyId,
                seriesId = command.seriesId,
                scheduleId = command.scheduleId
            )
        }

        val series = seriesDeferred.await()

        val affectedRoutesDeferred = async {
            routeRepository.findBySeries(
                companyId = command.companyId,
                seriesId = command.seriesId,
                fromDate = command.effectiveFrom,
                statuses = setOf(RouteStatus.PLANNED)
            )
        }

        AddChildValidationContext(
            series = series,
            schedule = scheduleDeferred.await(),
            child = childDeferred.await(),
            existingSeriesSchedule = existingScheduleDeferred.await(),
            affectedRoutes = affectedRoutesDeferred.await()
        )
    }
}