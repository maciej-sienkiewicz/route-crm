// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/getresult/GetOptimizationResultHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.getresult

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.*
import pl.sienkiewiczmaciej.routecrm.routeoptimization.external.GeoapifyOptimizationRequest
import pl.sienkiewiczmaciej.routecrm.routeoptimization.external.GeoapifyOptimizationResponse
import pl.sienkiewiczmaciej.routecrm.routeoptimization.optimize.OptimizationMetadata
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.LocalTime

data class GetOptimizationResultQuery(
    val companyId: CompanyId,
    val taskId: OptimizationTaskId
)

class OptimizationTaskNotFoundException(id: OptimizationTaskId) :
    NotFoundException("Optimization task ${id.value} not found")

@Component
class GetOptimizationResultHandler(
    private val optimizationTaskRepository: OptimizationTaskRepository,
    private val objectMapper: ObjectMapper,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetOptimizationResultQuery): OptimizationResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val task = optimizationTaskRepository.findById(query.companyId, query.taskId)
            ?: throw OptimizationTaskNotFoundException(query.taskId)

        if (task.status != OptimizationStatus.COMPLETED) {
            throw IllegalStateException("Task ${query.taskId.value} is not completed yet (status: ${task.status})")
        }

        val response = objectMapper.readValue<GeoapifyOptimizationResponse>(task.responseData!!)

        return parseOptimizationResponse(task, response)
    }

    private fun parseOptimizationResponse(
        task: OptimizationTask,
        response: GeoapifyOptimizationResponse
    ): OptimizationResult {
        // Parsuj request data z metadata
        val requestWrapper = objectMapper.readValue<Map<String, Any>>(task.requestData)

        val requestData = objectMapper.convertValue(
            requestWrapper["request"],
            GeoapifyOptimizationRequest::class.java
        )

        val metadata = objectMapper.convertValue(
            requestWrapper["metadata"],
            OptimizationMetadata::class.java
        )

        val routes = response.features.map { feature ->
            val agentPlan = feature.properties

            // Znajdź vehicleId i driverId z metadata
            val agentId = requestData.agents[agentPlan.agentIndex].id ?: "VEH-unknown"
            val vehicleDriverPair = metadata.vehicleMapping[agentId]

            val vehicleId = VehicleId.from(vehicleDriverPair?.vehicleId ?: agentId)
            val driverId = DriverId.from(vehicleDriverPair?.driverId ?: "DRV-unknown")

            val children = mutableListOf<OptimizedRouteChild>()
            var pickupOrder = 1

            agentPlan.actions
                .filter { it.type == "pickup" }
                .forEach { action ->
                    val waypoint = agentPlan.waypoints[action.waypointIndex]
                    val deliveryAction = agentPlan.actions.find {
                        it.type == "delivery" && it.shipmentId == action.shipmentId
                    }

                    if (deliveryAction != null && action.shipmentId != null) {
                        val deliveryWaypoint = agentPlan.waypoints[deliveryAction.waypointIndex]

                        // Parsuj shipment ID: "CH-xxx-SCH-yyy"
                        val childSchedulePair = metadata.childMapping[action.shipmentId]

                        if (childSchedulePair != null) {
                            children.add(
                                OptimizedRouteChild(
                                    childId = ChildId.from(childSchedulePair.childId),
                                    scheduleId = ScheduleId.from(childSchedulePair.scheduleId),
                                    pickupOrder = pickupOrder++,
                                    estimatedPickupTime = LocalTime.ofSecondOfDay(action.startTime.toLong()),
                                    estimatedDropoffTime = LocalTime.ofSecondOfDay(deliveryAction.startTime.toLong()),
                                    pickupLatitude = waypoint.location[1],
                                    pickupLongitude = waypoint.location[0],
                                    dropoffLatitude = deliveryWaypoint.location[1],
                                    dropoffLongitude = deliveryWaypoint.location[0]
                                )
                            )
                        }
                    }
                }

            OptimizedRoute(
                vehicleId = vehicleId,
                driverId = driverId,
                children = children,
                totalDistance = agentPlan.distance,
                totalTime = agentPlan.time,
                estimatedStartTime = LocalTime.ofSecondOfDay(agentPlan.startTime.toLong()),
                estimatedEndTime = LocalTime.ofSecondOfDay(agentPlan.endTime.toLong())
            )
        }

        val unassignedChildren = response.properties.issues?.unassignedShipments
            ?.mapNotNull { index ->
                requestData.shipments.getOrNull(index)?.let { shipment ->
                    val childSchedulePair = metadata.childMapping[shipment.id]
                    childSchedulePair?.let { ChildId.from(it.childId) }
                }
            }
            ?: emptyList()

        return OptimizationResult(
            taskId = task.id,
            companyId = task.companyId,
            date = task.date,
            routes = routes,
            unassignedChildren = unassignedChildren
        )
    }
}