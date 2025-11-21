// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/list/ListRouteSeriesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.list

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.routeseries.RouteSeriesListResponse
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesScheduleRepository
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class ListRouteSeriesQuery(
    val companyId: CompanyId,
    val status: RouteSeriesStatus?,
    val pageable: Pageable
)

@Component
class ListRouteSeriesHandler(
    private val routeSeriesRepository: RouteSeriesRepository,
    private val scheduleRepository: RouteSeriesScheduleRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(
        principal: UserPrincipal,
        query: ListRouteSeriesQuery
    ): Page<RouteSeriesListResponse> = coroutineScope {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val seriesPage = routeSeriesRepository.findAll(
            query.companyId,
            query.status,
            query.pageable
        )

        // Parallel loading of schedules for each series
        val responses = seriesPage.content.map { series ->
            async {
                val schedules = scheduleRepository.findAllBySeries(
                    query.companyId,
                    series.id
                )

                RouteSeriesListResponse(
                    id = series.id.value,
                    seriesName = series.seriesName,
                    recurrenceInterval = series.recurrenceInterval,  // ← POPRAWIONE
                    dayOfWeek = series.startDate.dayOfWeek.toString(),  // ← POPRAWIONE
                    startDate = series.startDate,
                    endDate = series.endDate,
                    status = series.status,
                    schedulesCount = schedules.size
                )
            }
        }.awaitAll()

        return@coroutineScope PageImpl(
            responses,
            seriesPage.pageable,
            seriesPage.totalElements
        )
    }
}