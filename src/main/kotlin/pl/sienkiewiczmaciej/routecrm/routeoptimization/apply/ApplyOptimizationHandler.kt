// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/apply/ApplyOptimizationHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.apply

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteCommand
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.create.RouteStopData
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.*
import pl.sienkiewiczmaciej.routecrm.routeoptimization.external.GeoapifyOptimizationRequest
import pl.sienkiewiczmaciej.routecrm.routeoptimization.external.GeoapifyOptimizationResponse
import pl.sienkiewiczmaciej.routecrm.routeoptimization.getresult.OptimizationTaskNotFoundException
import pl.sienkiewiczmaciej.routecrm.routeoptimization.optimize.OptimizationMetadata
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import java.time.LocalTime
import kotlin.jvm.optionals.getOrElse

data class ApplyOptimizationCommand(
    val companyId: CompanyId,
    val taskId: OptimizationTaskId
)

data class RouteCreationError(
    val routeIndex: Int,
    val vehicleId: String,
    val error: String
)

data class ApplyOptimizationResult(
    val createdRoutes: List<RouteId>,
    val successCount: Int,
    val failedCount: Int,
    val errors: List<RouteCreationError>
)

@Component
class ApplyOptimizationHandler(
    private val optimizationTaskRepository: OptimizationTaskRepository,
    private val createRouteHandler: CreateRouteHandler,
    private val scheduleRepository: ScheduleJpaRepository,
    private val objectMapper: ObjectMapper,
    private val authService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(ApplyOptimizationHandler::class.java)

    @Transactional
    suspend fun handle(principal: UserPrincipal, command: ApplyOptimizationCommand): ApplyOptimizationResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        logger.info("Applying optimization task ${command.taskId.value} for company ${command.companyId.value}")

        val task = optimizationTaskRepository.findById(command.companyId, command.taskId)
            ?: throw OptimizationTaskNotFoundException(command.taskId)

        if (task.status != OptimizationStatus.COMPLETED) {
            throw IllegalStateException("Cannot apply optimization: task is not completed (status: ${task.status})")
        }

        val response = objectMapper.readValue<GeoapifyOptimizationResponse>(task.responseData!!)

        val requestWrapper = objectMapper.readValue<Map<String, Any>>(task.requestData)
        val requestData = objectMapper.convertValue(
            requestWrapper["request"],
            GeoapifyOptimizationRequest::class.java
        )
        val metadata = objectMapper.convertValue(
            requestWrapper["metadata"],
            OptimizationMetadata::class.java
        )

        val results = createRoutesFromOptimization(
            principal = principal,
            companyId = command.companyId,
            date = task.date,
            response = response,
            requestData = requestData,
            metadata = metadata
        )

        val successCount = results.count { it.isSuccess }
        val failedCount = results.count { !it.isSuccess }
        val createdRoutes = results.mapNotNull { it.getOrNull() }
        val errors = results.mapIndexedNotNull { index, result ->
            result.exceptionOrNull()?.let { error ->
                val vehicleId = response.features.getOrNull(index)
                    ?.properties?.agentIndex?.let { agentIndex ->
                        requestData.agents.getOrNull(agentIndex)?.id ?: "unknown"
                    } ?: "unknown"

                RouteCreationError(
                    routeIndex = index,
                    vehicleId = vehicleId,
                    error = error.message ?: "Unknown error"
                )
            }
        }

        logger.info("Optimization applied: $successCount routes created, $failedCount failed")

        return ApplyOptimizationResult(
            createdRoutes = createdRoutes,
            successCount = successCount,
            failedCount = failedCount,
            errors = errors
        )
    }

    private suspend fun createRoutesFromOptimization(
        principal: UserPrincipal,
        companyId: CompanyId,
        date: java.time.LocalDate,
        response: GeoapifyOptimizationResponse,
        requestData: GeoapifyOptimizationRequest,
        metadata: OptimizationMetadata
    ): List<Result<RouteId>> = coroutineScope {
        response.features.mapIndexed { index, feature ->
            async {
                try {
                    val agentPlan = feature.properties
                    val agent = requestData.agents[agentPlan.agentIndex]

                    val vehicleDriverPair = metadata.vehicleMapping[agent.id]
                        ?: throw IllegalStateException("Vehicle mapping not found for agent ${agent.id}")

                    val vehicleId = VehicleId.from(vehicleDriverPair.vehicleId)
                    val driverId = DriverId.from(vehicleDriverPair.driverId)

                    val stops = mutableListOf<RouteStopData>()
                    var stopOrder = 1

                    // Przejdź przez wszystkie akcje pickup
                    agentPlan.actions.forEach { action ->
                        if (action.type == "pickup" && action.shipmentId != null) {
                            val waypoint = agentPlan.waypoints[action.waypointIndex]

                            val deliveryAction = agentPlan.actions.find {
                                it.type == "delivery" && it.shipmentId == action.shipmentId
                            }

                            if (deliveryAction != null) {
                                val deliveryWaypoint = agentPlan.waypoints[deliveryAction.waypointIndex]

                                val childSchedulePair = metadata.childMapping[action.shipmentId]
                                    ?: throw IllegalStateException("Child mapping not found for shipment ${action.shipmentId}")

                                val childId = ChildId.from(childSchedulePair.childId)
                                val scheduleId = ScheduleId.from(childSchedulePair.scheduleId)

                                val schedule = scheduleRepository.findById(scheduleId.value)
                                    .getOrElse { throw IllegalStateException("fdsfds") }

                                // Pickup stop
                                stops.add(
                                    RouteStopData(
                                        stopOrder = stopOrder++,
                                        stopType = StopType.PICKUP,
                                        childId = childId,
                                        scheduleId = scheduleId,
                                        estimatedTime = LocalTime.ofSecondOfDay(action.startTime.toLong()),
                                        address = ScheduleAddress(
                                            label = schedule.pickupAddressLabel,
                                            address = Address(
                                                street = schedule.pickupAddressStreet,
                                                houseNumber = schedule.pickupAddressHouseNumber,
                                                apartmentNumber = schedule.pickupAddressApartmentNumber,
                                                postalCode = schedule.pickupAddressPostalCode,
                                                city = schedule.pickupAddressCity
                                            ),
                                            latitude = waypoint.location[1],
                                            longitude = waypoint.location[0]
                                        )
                                    )
                                )

                                // Dropoff stop
                                stops.add(
                                    RouteStopData(
                                        stopOrder = stopOrder++,
                                        stopType = StopType.DROPOFF,
                                        childId = childId,
                                        scheduleId = scheduleId,
                                        estimatedTime = LocalTime.ofSecondOfDay(deliveryAction.startTime.toLong()),
                                        address = ScheduleAddress(
                                            label = schedule.dropoffAddressLabel,
                                            address = Address(
                                                street = schedule.dropoffAddressStreet,
                                                houseNumber = schedule.dropoffAddressHouseNumber,
                                                apartmentNumber = schedule.dropoffAddressApartmentNumber,
                                                postalCode = schedule.dropoffAddressPostalCode,
                                                city = schedule.dropoffAddressCity
                                            ),
                                            latitude = deliveryWaypoint.location[1],
                                            longitude = deliveryWaypoint.location[0]
                                        )
                                    )
                                )
                            }
                        }
                    }

                    if (stops.isEmpty()) {
                        throw IllegalStateException("No stops found for route $index")
                    }

                    val command = CreateRouteCommand(
                        companyId = companyId,
                        routeName = "Zoptymalizowana trasa ${index + 1} - ${date}",
                        date = date,
                        driverId = driverId,
                        vehicleId = vehicleId,
                        estimatedStartTime = LocalTime.ofSecondOfDay(agentPlan.startTime.toLong()),
                        estimatedEndTime = LocalTime.ofSecondOfDay(agentPlan.endTime.toLong()),
                        stops = stops
                    )

                    val result = createRouteHandler.handle(principal, command)

                    logger.info("Created route ${result.id.value} for vehicle ${vehicleId.value} with ${stops.size} stops (distance: ${agentPlan.distance}m)")

                    Result.success(result.id)
                } catch (e: Exception) {
                    logger.error("Failed to create route $index: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }.awaitAll()
    }
}