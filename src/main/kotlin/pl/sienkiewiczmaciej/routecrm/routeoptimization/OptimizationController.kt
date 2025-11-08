// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/OptimizationController.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.routeoptimization.apply.ApplyOptimizationHandler
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationStatus
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationTaskId
import pl.sienkiewiczmaciej.routecrm.routeoptimization.getresult.GetOptimizationResultHandler
import pl.sienkiewiczmaciej.routecrm.routeoptimization.getresult.GetOptimizationResultQuery
import pl.sienkiewiczmaciej.routecrm.routeoptimization.optimize.OptimizeRoutesCommand
import pl.sienkiewiczmaciej.routecrm.routeoptimization.optimize.OptimizeRoutesHandler
import pl.sienkiewiczmaciej.routecrm.routeoptimization.optimize.RouteType
import pl.sienkiewiczmaciej.routecrm.routeoptimization.apply.ApplyOptimizationCommand
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import java.time.LocalDate
import java.time.LocalTime

data class OptimizeRoutesRequest(
    @field:NotNull(message = "Date is required")
    val date: LocalDate,

    @field:NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    val startTime: LocalTime,

    @field:NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    val endTime: LocalTime,

    val routeType: RouteType = RouteType.ALL
)

data class OptimizeRoutesResponse(
    val taskId: String,
    val status: OptimizationStatus,
    val message: String
)

data class OptimizedRouteChildResponse(
    val childId: String,
    val scheduleId: String,
    val pickupOrder: Int,
    @JsonFormat(pattern = "HH:mm")
    val estimatedPickupTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedDropoffTime: LocalTime,
    val pickupLocation: LocationResponse,
    val dropoffLocation: LocationResponse
)

data class LocationResponse(
    val latitude: Double,
    val longitude: Double
)

data class OptimizedRouteResponse(
    val vehicleId: String,
    val driverId: String,
    val children: List<OptimizedRouteChildResponse>,
    val totalDistanceMeters: Int,
    val totalTimeSeconds: Int,
    @JsonFormat(pattern = "HH:mm")
    val estimatedStartTime: LocalTime,
    @JsonFormat(pattern = "HH:mm")
    val estimatedEndTime: LocalTime
)

data class OptimizationResultResponse(
    val taskId: String,
    val companyId: String,
    val date: LocalDate,
    val routes: List<OptimizedRouteResponse>,
    val unassignedChildren: List<String>
)

data class ApplyOptimizationResponse(
    val createdRoutes: List<String>,
    val successCount: Int,
    val failedCount: Int,
    val errors: List<String>,
    val message: String
)

@RestController
@RequestMapping("/api/route-optimization")
class OptimizationController(
    private val optimizeHandler: OptimizeRoutesHandler,
    private val getResultHandler: GetOptimizationResultHandler,
    private val applyOptimizationHandler: ApplyOptimizationHandler
) : BaseController() {

    @PostMapping("/optimize")
    suspend fun optimize(
        @Valid @RequestBody request: OptimizeRoutesRequest
    ): ResponseEntity<OptimizeRoutesResponse> {
        val principal = getPrincipal()
        val command = OptimizeRoutesCommand(
            companyId = principal.companyId,
            date = request.date,
            startTime = request.startTime,
            endTime = request.endTime,
            routeType = request.routeType
        )

        val result = optimizeHandler.handle(principal, command)

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            OptimizeRoutesResponse(
                taskId = result.taskId.value,
                status = result.status,
                message = result.message
            )
        )
    }

    @GetMapping("/tasks/{taskId}")
    suspend fun getResult(@PathVariable taskId: String): OptimizationResultResponse {
        val principal = getPrincipal()
        val query = GetOptimizationResultQuery(
            companyId = principal.companyId,
            taskId = OptimizationTaskId.from(taskId)
        )

        val result = getResultHandler.handle(principal, query)

        return OptimizationResultResponse(
            taskId = result.taskId.value,
            companyId = result.companyId.value,
            date = result.date,
            routes = result.routes.map { route ->
                OptimizedRouteResponse(
                    vehicleId = route.vehicleId.value,
                    driverId = route.driverId.value,
                    children = route.children.map { child ->
                        OptimizedRouteChildResponse(
                            childId = child.childId.value,
                            scheduleId = child.scheduleId.value,
                            pickupOrder = child.pickupOrder,
                            estimatedPickupTime = child.estimatedPickupTime,
                            estimatedDropoffTime = child.estimatedDropoffTime,
                            pickupLocation = LocationResponse(
                                latitude = child.pickupLatitude,
                                longitude = child.pickupLongitude
                            ),
                            dropoffLocation = LocationResponse(
                                latitude = child.dropoffLatitude,
                                longitude = child.dropoffLongitude
                            )
                        )
                    },
                    totalDistanceMeters = route.totalDistance,
                    totalTimeSeconds = route.totalTime,
                    estimatedStartTime = route.estimatedStartTime,
                    estimatedEndTime = route.estimatedEndTime
                )
            },
            unassignedChildren = result.unassignedChildren.map { it.value }
        )
    }

    @PostMapping("/tasks/{taskId}/apply")
    suspend fun applyOptimization(@PathVariable taskId: String): ApplyOptimizationResponse {
        val principal = getPrincipal()
        val command = ApplyOptimizationCommand(
            companyId = principal.companyId,
            taskId = OptimizationTaskId.from(taskId)
        )

        val result = applyOptimizationHandler.handle(principal, command)

        val message = when {
            result.failedCount == 0 -> "All ${result.successCount} routes created successfully"
            result.successCount == 0 -> "Failed to create any routes"
            else -> "${result.successCount} routes created successfully, ${result.failedCount} failed"
        }

        return ApplyOptimizationResponse(
            createdRoutes = result.createdRoutes.map { it.value },
            successCount = result.successCount,
            failedCount = result.failedCount,
            errors = result.errors,
            message = message
        )
    }
}