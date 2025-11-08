// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/domain/RouteOptimizationModels.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@JvmInline
value class OptimizationTaskId(val value: String) {
    companion object {
        fun generate() = OptimizationTaskId("OPT-${UUID.randomUUID()}")
        fun from(value: String) = OptimizationTaskId(value)
    }
}

enum class OptimizationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class OptimizationTask(
    val id: OptimizationTaskId,
    val companyId: CompanyId,
    val date: LocalDate,
    val status: OptimizationStatus,
    val requestData: String,
    val responseData: String?,
    val errorMessage: String?,
    val createdAt: Instant,
    val completedAt: Instant?
) {
    companion object {
        fun create(
            companyId: CompanyId,
            date: LocalDate,
            requestData: String
        ): OptimizationTask {
            return OptimizationTask(
                id = OptimizationTaskId.generate(),
                companyId = companyId,
                date = date,
                status = OptimizationStatus.PENDING,
                requestData = requestData,
                responseData = null,
                errorMessage = null,
                createdAt = Instant.now(),
                completedAt = null
            )
        }
    }

    fun markProcessing(): OptimizationTask {
        require(status == OptimizationStatus.PENDING) {
            "Only PENDING tasks can be marked as PROCESSING"
        }
        return copy(status = OptimizationStatus.PROCESSING)
    }

    fun markCompleted(responseData: String): OptimizationTask {
        require(status == OptimizationStatus.PROCESSING) {
            "Only PROCESSING tasks can be marked as COMPLETED"
        }
        return copy(
            status = OptimizationStatus.COMPLETED,
            responseData = responseData,
            completedAt = Instant.now()
        )
    }

    fun markFailed(errorMessage: String): OptimizationTask {
        return copy(
            status = OptimizationStatus.FAILED,
            errorMessage = errorMessage,
            completedAt = Instant.now()
        )
    }
}

data class OptimizedRouteChild(
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val pickupOrder: Int,
    val estimatedPickupTime: LocalTime,
    val estimatedDropoffTime: LocalTime,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val dropoffLatitude: Double,
    val dropoffLongitude: Double
)

data class OptimizedRoute(
    val vehicleId: VehicleId,
    val driverId: DriverId,
    val children: List<OptimizedRouteChild>,
    val totalDistance: Int,
    val totalTime: Int,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime
)

data class OptimizationResult(
    val taskId: OptimizationTaskId,
    val companyId: CompanyId,
    val date: LocalDate,
    val routes: List<OptimizedRoute>,
    val unassignedChildren: List<ChildId>
)