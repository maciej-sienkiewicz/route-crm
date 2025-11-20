// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/suggestions/GetRouteSuggestionsValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.suggestions

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.Route
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.services.RouteStops
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.ScheduleNotFoundException

/**
 * Validation context for GetRouteSuggestions operation.
 */
data class GetRouteSuggestionsValidationContext(
    val schedule: Schedule,
    val availableRoutes: List<Route>,
    val routeStopsMap: List<RouteStops>
)

@Component
class GetRouteSuggestionsContextBuilder(
    private val scheduleRepository: ScheduleRepository,
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository
) {
    suspend fun build(query: GetRouteSuggestionsQuery): GetRouteSuggestionsValidationContext = coroutineScope {
        // 1. Load schedule
        val scheduleDeferred = async {
            scheduleRepository.findById(query.companyId, query.scheduleId)
                ?: throw ScheduleNotFoundException(query.scheduleId)
        }

        // 2. Load available routes for the given date (PLANNED status only)
        val routesDeferred = async {
            routeRepository.findAll(
                companyId = query.companyId,
                date = query.date,
                status = RouteStatus.PLANNED,
                driverId = null,
                pageable = Pageable.unpaged()
            ).content
        }

        val schedule = scheduleDeferred.await()
        val routes = routesDeferred.await()

        // 3. Load stops for each route
        val routeStopsMap = routes.map { route ->
            async {
                val stops = stopRepository.findByRoute(
                    companyId = query.companyId,
                    routeId = route.id,
                    includeCancelled = false
                )
                RouteStops(
                    routeId = route.id,
                    stops = stops
                )
            }
        }.map { it.await() }

        GetRouteSuggestionsValidationContext(
            schedule = schedule,
            availableRoutes = routes,
            routeStopsMap = routeStopsMap
        )
    }
}

/**
 * Composite validator for GetRouteSuggestions operation.
 */
@Component
class GetRouteSuggestionsValidatorComposite(
    private val contextBuilder: GetRouteSuggestionsContextBuilder,
    private val scheduleActiveValidator: GetSuggestionsScheduleActiveValidator,
    private val scheduleHasCoordinatesValidator: GetSuggestionsScheduleHasCoordinatesValidator,
    private val maxResultsValidator: GetSuggestionsMaxResultsValidator
) {
    suspend fun validate(query: GetRouteSuggestionsQuery): GetRouteSuggestionsValidationContext {
        // 1. Validate max results before building context
        maxResultsValidator.validate(query)

        // 2. Build context
        val context = contextBuilder.build(query)

        // 3. Run validators
        scheduleActiveValidator.validate(context)
        scheduleHasCoordinatesValidator.validate(context)

        // 4. Return context
        return context
    }
}

@Component
class GetSuggestionsScheduleActiveValidator {
    fun validate(context: GetRouteSuggestionsValidationContext) {
        require(context.schedule.active) {
            "Cannot find suggestions for inactive schedule ${context.schedule.id.value}"
        }
    }
}

@Component
class GetSuggestionsScheduleHasCoordinatesValidator {
    fun validate(context: GetRouteSuggestionsValidationContext) {
        require(
            context.schedule.pickupAddress.latitude != null &&
                    context.schedule.pickupAddress.longitude != null
        ) {
            "Schedule ${context.schedule.id.value} pickup address must have coordinates"
        }

        require(
            context.schedule.dropoffAddress.latitude != null &&
                    context.schedule.dropoffAddress.longitude != null
        ) {
            "Schedule ${context.schedule.id.value} dropoff address must have coordinates"
        }
    }
}

@Component
class GetSuggestionsMaxResultsValidator {
    fun validate(query: GetRouteSuggestionsQuery) {
        require(query.maxResults > 0) {
            "Max results must be greater than 0"
        }
        require(query.maxResults <= 50) {
            "Max results cannot exceed 50"
        }
    }
}