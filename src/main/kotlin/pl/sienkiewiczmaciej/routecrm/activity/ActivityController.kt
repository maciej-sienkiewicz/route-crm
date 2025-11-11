// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/activity/ActivityController.kt
package pl.sienkiewiczmaciej.routecrm.activity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.activity.domain.ActivityCategory
import pl.sienkiewiczmaciej.routecrm.activity.list.ListActivitiesHandler
import pl.sienkiewiczmaciej.routecrm.activity.list.ListActivitiesQuery
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api")
class ActivityController(
    private val listActivitiesHandler: ListActivitiesHandler
) : BaseController() {

    @GetMapping("/children/{childId}/activities")
    suspend fun getChildActivities(
        @PathVariable childId: String,
        @RequestParam(required = false) category: ActivityCategory?,
        @PageableDefault(size = 20, sort = ["timestamp"]) pageable: Pageable
    ): Page<ActivityResponse> {
        val principal = getPrincipal()
        val query = ListActivitiesQuery(
            companyId = principal.companyId,
            aggregateId = childId,
            category = category,
            pageable = pageable
        )
        return listActivitiesHandler.handle(principal, query)
            .map { ActivityResponse.from(it) }
    }

    @GetMapping("/guardians/{guardianId}/activities")
    suspend fun getGuardianActivities(
        @PathVariable guardianId: String,
        @RequestParam(required = false) category: ActivityCategory?,
        @PageableDefault(size = 20, sort = ["timestamp"]) pageable: Pageable
    ): Page<ActivityResponse> {
        val principal = getPrincipal()
        val query = ListActivitiesQuery(
            companyId = principal.companyId,
            aggregateId = guardianId,
            category = category,
            pageable = pageable
        )
        return listActivitiesHandler.handle(principal, query)
            .map { ActivityResponse.from(it) }
    }

    @GetMapping("/routes/{routeId}/activities")
    suspend fun getRouteActivities(
        @PathVariable routeId: String,
        @RequestParam(required = false) category: ActivityCategory?,
        @PageableDefault(size = 20, sort = ["timestamp"]) pageable: Pageable
    ): Page<ActivityResponse> {
        val principal = getPrincipal()
        val query = ListActivitiesQuery(
            companyId = principal.companyId,
            aggregateId = routeId,
            category = category,
            pageable = pageable
        )
        return listActivitiesHandler.handle(principal, query)
            .map { ActivityResponse.from(it) }
    }

    @GetMapping("/drivers/{driverId}/activities")
    suspend fun getDriverActivities(
        @PathVariable driverId: String,
        @RequestParam(required = false) category: ActivityCategory?,
        @PageableDefault(size = 20, sort = ["timestamp"]) pageable: Pageable
    ): Page<ActivityResponse> {
        val principal = getPrincipal()
        val query = ListActivitiesQuery(
            companyId = principal.companyId,
            aggregateId = driverId,
            category = category,
            pageable = pageable
        )
        return listActivitiesHandler.handle(principal, query)
            .map { ActivityResponse.from(it) }
    }

    @GetMapping("/schedules/{scheduleId}/activities")
    suspend fun getScheduleActivities(
        @PathVariable scheduleId: String,
        @RequestParam(required = false) category: ActivityCategory?,
        @PageableDefault(size = 20, sort = ["timestamp"]) pageable: Pageable
    ): Page<ActivityResponse> {
        val principal = getPrincipal()
        val query = ListActivitiesQuery(
            companyId = principal.companyId,
            aggregateId = scheduleId,
            category = category,
            pageable = pageable
        )
        return listActivitiesHandler.handle(principal, query)
            .map { ActivityResponse.from(it) }
    }
}