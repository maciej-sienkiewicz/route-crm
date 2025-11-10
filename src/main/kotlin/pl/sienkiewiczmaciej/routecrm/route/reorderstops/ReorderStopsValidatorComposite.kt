// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/reorderstops/ReorderStopsValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.reorderstops

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus

/**
 * Composite validator for ReorderStops operation.
 */
@Component
class ReorderStopsValidatorComposite(
    private val contextBuilder: ReorderStopsContextBuilder,
    private val routeStatusValidator: ReorderStopsRouteStatusValidator,
    private val allStopsExistValidator: ReorderStopsAllStopsExistValidator,
    private val orderPositiveValidator: ReorderStopsOrderPositiveValidator,
    private val noDuplicateOrdersValidator: ReorderStopsNoDuplicateOrdersValidator,
    private val stopsModifiableValidator: ReorderStopsModifiableValidator
) {
    suspend fun validate(command: ReorderRouteStopsCommand): ReorderStopsValidationContext {
        // 1. Build context
        val context = contextBuilder.build(command)

        // 2. Run validators
        routeStatusValidator.validate(context)
        allStopsExistValidator.validate(command, context)
        orderPositiveValidator.validate(command)
        noDuplicateOrdersValidator.validate(command)
        stopsModifiableValidator.validate(command, context)

        // 3. Return context
        return context
    }
}

@Component
class ReorderStopsRouteStatusValidator {
    fun validate(context: ReorderStopsValidationContext) {
        require(context.route.status == RouteStatus.PLANNED) {
            "Cannot reorder stops in route with status ${context.route.status}"
        }
    }
}

@Component
class ReorderStopsAllStopsExistValidator {
    fun validate(command: ReorderRouteStopsCommand, context: ReorderStopsValidationContext) {
        val stopIds = context.existingStops.map { it.id }.toSet()
        command.stopOrders.forEach { update ->
            require(stopIds.contains(update.stopId)) {
                "Stop ${update.stopId.value} not found in route"
            }
        }
    }
}

@Component
class ReorderStopsOrderPositiveValidator {
    fun validate(command: ReorderRouteStopsCommand) {
        command.stopOrders.forEach { update ->
            require(update.newOrder > 0) {
                "Stop order must be positive"
            }
        }
    }
}

@Component
class ReorderStopsNoDuplicateOrdersValidator {
    fun validate(command: ReorderRouteStopsCommand) {
        val newOrders = command.stopOrders.map { it.newOrder }
        require(newOrders.toSet().size == newOrders.size) {
            "Duplicate stop orders are not allowed"
        }
    }
}

@Component
class ReorderStopsModifiableValidator {
    fun validate(command: ReorderRouteStopsCommand, context: ReorderStopsValidationContext) {
        val stopMap = context.existingStops.associateBy { it.id }
        command.stopOrders.forEach { update ->
            val stop = stopMap[update.stopId]!!
            require(stop.canBeModified()) {
                "Cannot reorder stop ${stop.id.value}: ${
                    when {
                        stop.isExecuted() -> "already executed"
                        stop.isCancelled -> "cancelled"
                        else -> "not modifiable"
                    }
                }"
            }
        }
    }
}