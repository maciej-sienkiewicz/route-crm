// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/suggestions/GetRouteSuggestionsHandler.kt
package pl.sienkiewiczmaciej.routecrm.route.suggestions

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.services.RouteSuggestionService
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class GetRouteSuggestionsQuery(
    val companyId: CompanyId,
    val scheduleId: ScheduleId,
    val date: LocalDate,
    val maxResults: Int
)

/**
 * Handler for getting route suggestions based on schedule location.
 * Uses RouteSuggestionService to find routes that pass near the schedule's pickup and dropoff points.
 */
@Component
class GetRouteSuggestionsHandler(
    private val validatorComposite: GetRouteSuggestionsValidatorComposite,
    private val routeSuggestionService: RouteSuggestionService,
    private val enrichmentService: RouteSuggestionEnrichmentService,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetRouteSuggestionsQuery): List<RouteSuggestionDetail> {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        // 2. Validate (throws exception on failure, returns context)
        val context = validatorComposite.validate(query)

        // 3. Find suggestions using domain service
        val suggestedRouteIds = routeSuggestionService.findSuggestions(
            routes = context.routeStopsMap,
            newSchedule = context.schedule
        )

        // 4. Limit results to maxResults
        val limitedRouteIds = suggestedRouteIds.take(query.maxResults)

        // 5. Enrich with route details
        val enrichedResults = enrichmentService.enrichRouteSuggestions(
            routeIds = limitedRouteIds,
            availableRoutes = context.availableRoutes,
            companyId = query.companyId
        )

        return enrichedResults
    }
}