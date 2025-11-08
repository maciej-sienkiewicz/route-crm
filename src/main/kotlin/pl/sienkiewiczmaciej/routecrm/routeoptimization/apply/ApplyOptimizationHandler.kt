// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/apply/ApplyOptimizationHandler.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.apply

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.RoutePointRequest
import pl.sienkiewiczmaciej.routecrm.route.RoutePointType
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteCommand
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteHandler
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.routeoptimization.domain.OptimizationTaskId
import pl.sienkiewiczmaciej.routecrm.routeoptimization.getresult.GetOptimizationResultHandler
import pl.sienkiewiczmaciej.routecrm.routeoptimization.getresult.GetOptimizationResultQuery
import pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class ApplyOptimizationCommand(
    val companyId: CompanyId,
    val taskId: OptimizationTaskId
)

data class ApplyOptimizationResult(
    val createdRoutes: List<RouteId>,
    val successCount: Int,
    val failedCount: Int,
    val errors: List<String>
)

@Component
class ApplyOptimizationHandler(
    private val getResultHandler: GetOptimizationResultHandler,
    private val createRouteHandler: CreateRouteHandler,
    private val scheduleRepository: ScheduleJpaRepository,
    private val authService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(ApplyOptimizationHandler::class.java)

    @Transactional
    suspend fun handle(principal: UserPrincipal, command: ApplyOptimizationCommand): ApplyOptimizationResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val optimizationResult = getResultHandler.handle(
            principal,
            GetOptimizationResultQuery(command.companyId, command.taskId)
        )

        val createdRoutes = mutableListOf<RouteId>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0

        optimizationResult.routes.forEachIndexed { index, optimizedRoute ->
            try {
                logger.info("Creating route ${index + 1} with ${optimizedRoute.children.size} children")

                // Konwertuj OptimizedRouteChild na RoutePointRequest (format API)
                val points = optimizedRoute.children.flatMap { child ->
                    // Sprawdź czy harmonogram istnieje
                    val schedule = withContext(Dispatchers.IO) {
                        scheduleRepository.findByIdAndCompanyId(
                            child.scheduleId.value,
                            command.companyId.value
                        ) ?: throw IllegalArgumentException(
                            "Schedule ${child.scheduleId.value} not found for child ${child.childId.value}"
                        )
                    }

                    listOf(
                        // Punkt PICKUP
                        RoutePointRequest(
                            childId = child.childId.value,
                            scheduleId = child.scheduleId.value,
                            type = RoutePointType.PICKUP,
                            order = child.pickupOrder,
                            estimatedTime = child.estimatedPickupTime
                        ),
                        // Punkt DROPOFF
                        RoutePointRequest(
                            childId = child.childId.value,
                            scheduleId = child.scheduleId.value,
                            type = RoutePointType.DROPOFF,
                            order = child.pickupOrder, // Użyj tego samego order - walidacja w handlerze
                            estimatedTime = child.estimatedDropoffTime
                        )
                    )
                }

                val createCommand = CreateRouteCommand(
                    companyId = command.companyId,
                    routeName = "Optimized Route ${index + 1} - ${optimizationResult.date}",
                    date = optimizationResult.date,
                    driverId = optimizedRoute.driverId,
                    vehicleId = optimizedRoute.vehicleId,
                    estimatedStartTime = optimizedRoute.estimatedStartTime,
                    estimatedEndTime = optimizedRoute.estimatedEndTime,
                    points = points
                )

                val result = createRouteHandler.handle(principal, createCommand)
                createdRoutes.add(result.id)
                successCount++

                logger.info("Successfully created route ${result.id.value} with ${result.childrenCount} children")
            } catch (e: Exception) {
                failedCount++
                val errorMsg = "Failed to create route ${index + 1}: ${e.message}"
                errors.add(errorMsg)
                logger.error(errorMsg, e)
            }
        }

        logger.info("Apply optimization completed: $successCount successful, $failedCount failed")

        return ApplyOptimizationResult(
            createdRoutes = createdRoutes,
            successCount = successCount,
            failedCount = failedCount,
            errors = errors
        )
    }
}