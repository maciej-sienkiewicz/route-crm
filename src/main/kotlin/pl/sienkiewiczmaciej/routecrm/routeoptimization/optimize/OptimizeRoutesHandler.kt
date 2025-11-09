// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/optimize/OptimizeRoutesHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.optimize

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.*
import pl.sienkiewiczmaciej.routecrm.routeoptimization.external.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleStatus
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.LocalDate
import java.time.LocalTime

data class OptimizeRoutesCommand(
    val companyId: CompanyId,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val routeType: RouteType = RouteType.ALL
)

enum class RouteType {
    ALL,
    MORNING,
    AFTERNOON
}

data class VehicleAvailability(
    val vehicleId: VehicleId,
    val driverId: DriverId,
    val capacity: Int,
    val wheelchairCapacity: Int,
    val childSeatCapacity: Int,
    val baseLatitude: Double,
    val baseLongitude: Double
)

data class ChildScheduleInfo(
    val childId: ChildId,
    val scheduleId: String,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val dropoffLatitude: Double,
    val dropoffLongitude: Double,
    val pickupTime: LocalTime,
    val dropoffTime: LocalTime,
    val pickupDuration: Int,
    val dropoffDuration: Int,
    val requiresWheelchair: Boolean,
    val requiresChildSeat: Boolean
) {
    fun getUniqueShipmentId(): String {
        return "${childId.value}-SCH-$scheduleId"
    }
}

data class OptimizeRoutesResult(
    val taskId: OptimizationTaskId,
    val status: OptimizationStatus,
    val message: String
)

@Component
class OptimizeRoutesHandler(
    private val optimizationTaskRepository: OptimizationTaskRepository,
    private val vehicleRepository: VehicleJpaRepository,
    private val driverRepository: DriverJpaRepository,
    private val scheduleRepository: ScheduleJpaRepository,
    private val geoapifyClient: GeoapifyRouteOptimizerClient,
    private val objectMapper: ObjectMapper,
    private val authService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(OptimizeRoutesHandler::class.java)

    @Transactional
    suspend fun handle(principal: UserPrincipal, command: OptimizeRoutesCommand): OptimizeRoutesResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        logger.info("Starting route optimization for company ${command.companyId.value} on ${command.date}, type: ${command.routeType}")

        val availableVehicles = getAvailableVehicles(command.companyId, command.date)
        logger.info("Found ${availableVehicles.size} available vehicles")

        if (availableVehicles.isEmpty()) {
            logger.warn("No available vehicles found for optimization")
            return OptimizeRoutesResult(
                taskId = OptimizationTaskId.generate(),
                status = OptimizationStatus.FAILED,
                message = "No available vehicles found for the specified date"
            )
        }

        val childSchedules = getChildSchedules(command.companyId, command.date, command.routeType)
        logger.info("Found ${childSchedules.size} child schedules")

        if (childSchedules.isEmpty()) {
            logger.warn("No child schedules found for optimization")
            return OptimizeRoutesResult(
                taskId = OptimizationTaskId.generate(),
                status = OptimizationStatus.FAILED,
                message = "No child schedules found for the specified date and route type"
            )
        }

        val uniqueChildren = childSchedules.map { it.childId }.toSet()
        logger.info("Unique children: ${uniqueChildren.size}, Total schedules: ${childSchedules.size}")

        val geoapifyRequest = buildGeoapifyRequest(
            availableVehicles,
            childSchedules,
            command.startTime,
            command.endTime
        )

        val metadata = OptimizationMetadata(
            vehicleMapping = availableVehicles.associate {
                it.vehicleId.value to VehicleDriverPair(
                    it.vehicleId.value,
                    it.driverId.value
                )
            },
            childMapping = childSchedules.associate {
                it.getUniqueShipmentId() to ChildSchedulePair(
                    it.childId.value,
                    it.scheduleId
                )
            }
        )

        val taskData = mapOf(
            "request" to geoapifyRequest,
            "metadata" to metadata
        )
        val requestJson = objectMapper.writeValueAsString(taskData)

        val task = OptimizationTask.create(
            companyId = command.companyId,
            date = command.date,
            requestData = requestJson
        )
        val savedTask = optimizationTaskRepository.save(task)

        processOptimizationAsync(savedTask, geoapifyRequest)

        return OptimizeRoutesResult(
            taskId = savedTask.id,
            status = savedTask.status,
            message = "Optimization task created successfully"
        )
    }

    private suspend fun getAvailableVehicles(
        companyId: CompanyId,
        date: LocalDate
    ): List<VehicleAvailability> = withContext(Dispatchers.IO) {
        val vehicles = vehicleRepository.findByCompanyIdAndStatus(
            companyId.value,
            VehicleStatus.AVAILABLE,
            Pageable.unpaged()
        )

        val drivers = driverRepository.findByCompanyIdAndStatus(
            companyId.value,
            DriverStatus.ACTIVE,
            Pageable.unpaged()
        ).content.toMutableList()

        vehicles.content.mapNotNull { vehicle ->
            val driver = drivers.firstOrNull()

            if (driver != null) {
                drivers.remove(driver)

                VehicleAvailability(
                    vehicleId = VehicleId.from(vehicle.id),
                    driverId = DriverId.from(driver.id),
                    capacity = vehicle.capacityTotalSeats,
                    wheelchairCapacity = vehicle.capacityWheelchairSpaces,
                    childSeatCapacity = vehicle.capacityChildSeats,
                    baseLatitude = 52.4064,
                    baseLongitude = 16.9252
                )
            } else {
                logger.warn("No available driver for vehicle ${vehicle.id}")
                null
            }
        }
    }

    private suspend fun getChildSchedules(
        companyId: CompanyId,
        date: LocalDate,
        routeType: RouteType
    ): List<ChildScheduleInfo> = withContext(Dispatchers.IO) {
        val dayOfWeek = convertToDayOfWeek(date)

        val schedules = scheduleRepository.findAll().filter { schedule ->
            schedule.companyId == companyId.value &&
                    schedule.active &&
                    schedule.days.contains(dayOfWeek)
        }

        val filteredSchedules = when (routeType) {
            RouteType.MORNING -> schedules.filter { it.pickupTime.hour < 12 }
            RouteType.AFTERNOON -> schedules.filter { it.pickupTime.hour >= 12 }
            RouteType.ALL -> schedules
        }

        filteredSchedules.mapNotNull { schedule ->
            val pickupLat = schedule.pickupLatitude
            val pickupLon = schedule.pickupLongitude
            val dropoffLat = schedule.dropoffLatitude
            val dropoffLon = schedule.dropoffLongitude

            if (pickupLat == null || pickupLon == null || dropoffLat == null || dropoffLon == null) {
                logger.warn("Schedule ${schedule.id} missing geocoding data, skipping")
                return@mapNotNull null
            }

            ChildScheduleInfo(
                childId = ChildId.from(schedule.childId),
                scheduleId = schedule.id,
                pickupLatitude = pickupLat,
                pickupLongitude = pickupLon,
                dropoffLatitude = dropoffLat,
                dropoffLongitude = dropoffLon,
                pickupTime = schedule.pickupTime,
                dropoffTime = schedule.dropoffTime,
                pickupDuration = 120,
                dropoffDuration = 120,
                requiresWheelchair = false,
                requiresChildSeat = false
            )
        }
    }

    private fun buildGeoapifyRequest(
        vehicles: List<VehicleAvailability>,
        children: List<ChildScheduleInfo>,
        startTime: LocalTime,
        endTime: LocalTime
    ): GeoapifyOptimizationRequest {
        val startTimeSeconds = startTime.toSecondOfDay()
        val endTimeSeconds = endTime.toSecondOfDay()
        val workingTimeSeconds = endTimeSeconds - startTimeSeconds

        val agents = vehicles.map { vehicle ->
            GeoapifyAgent(
                startLocation = listOf(vehicle.baseLongitude, vehicle.baseLatitude),
                endLocation = listOf(vehicle.baseLongitude, vehicle.baseLatitude),
                timeWindows = listOf(listOf(0, workingTimeSeconds)),
                deliveryCapacity = vehicle.capacity,
                capabilities = buildList {
                    add("vehicle-${vehicle.vehicleId.value}")
                    if (vehicle.wheelchairCapacity > 0) add("wheelchair")
                    if (vehicle.childSeatCapacity > 0) add("child-seat")
                },
                id = vehicle.vehicleId.value,
                description = "Vehicle ${vehicle.vehicleId.value}"
            )
        }

        // Dla najkrótszej drogi - bardzo szerokie time windows lub ich brak
        val shipments = children.map { child ->
            GeoapifyShipment(
                id = child.getUniqueShipmentId(),
                pickup = GeoapifyPickup(
                    location = listOf(child.pickupLongitude, child.pickupLatitude),
                    duration = child.pickupDuration,
                    timeWindows = null // Bez ograniczeń czasowych
                ),
                delivery = GeoapifyDelivery(
                    location = listOf(child.dropoffLongitude, child.dropoffLatitude),
                    duration = child.dropoffDuration,
                    timeWindows = null // Bez ograniczeń czasowych
                ),
                amount = 1,
                priority = null, // Bez priorytetów - wszystkie równe
                requirements = buildList {
                    if (child.requiresWheelchair) add("wheelchair")
                    if (child.requiresChildSeat) add("child-seat")
                },
                description = "Child ${child.childId.value} - Schedule ${child.scheduleId}"
            )
        }

        logger.info("Created ${agents.size} agents and ${shipments.size} shipments for optimization")
        logger.info("Optimization type: shortest (minimizing distance)")

        return GeoapifyOptimizationRequest(
            mode = "drive",
            agents = agents,
            shipments = shipments,
            traffic = "approximated",
            type = "shortest" // NAJWAŻNIEJSZE: Optymalizacja pod kątem najkrótszej drogi
        )
    }

    private suspend fun processOptimizationAsync(
        task: OptimizationTask,
        request: GeoapifyOptimizationRequest
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val processingTask = task.markProcessing()
                optimizationTaskRepository.save(processingTask)

                val response = geoapifyClient.optimizeRoutes(request)

                if (response != null) {
                    val responseJson = objectMapper.writeValueAsString(response)
                    val completedTask = processingTask.markCompleted(responseJson)
                    optimizationTaskRepository.save(completedTask)

                    logger.info("Optimization task ${task.id.value} completed successfully")
                } else {
                    val failedTask = processingTask.markFailed("Failed to get response from Geoapify")
                    optimizationTaskRepository.save(failedTask)

                    logger.error("Optimization task ${task.id.value} failed")
                }
            } catch (e: Exception) {
                logger.error("Error processing optimization task ${task.id.value}: ${e.message}", e)
                val failedTask = task.markFailed(e.message ?: "Unknown error")
                optimizationTaskRepository.save(failedTask)
            }
        }
    }

    private fun convertToDayOfWeek(date: LocalDate): DayOfWeek {
        return when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
            java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }
}

data class OptimizationMetadata(
    val vehicleMapping: Map<String, VehicleDriverPair>,
    val childMapping: Map<String, ChildSchedulePair>
)

data class VehicleDriverPair(
    val vehicleId: String,
    val driverId: String
)

data class ChildSchedulePair(
    val childId: String,
    val scheduleId: String
)
