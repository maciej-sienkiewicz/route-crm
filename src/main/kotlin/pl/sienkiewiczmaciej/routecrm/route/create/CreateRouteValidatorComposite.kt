// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/create/CreateRouteValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.route.create

import org.springframework.stereotype.Component

/**
 * Composite validator that orchestrates all CreateRoute validation rules.
 * Builds the validation context and runs all validators in a defined order.
 */
@Component
class CreateRouteValidatorComposite(
    private val contextBuilder: CreateRouteValidationContextBuilder,
    private val stopOrderValidator: StopOrderValidator,
    private val driverStatusValidator: DriverStatusValidator,
    private val driverConflictValidator: DriverConflictValidator,
    private val vehicleStatusValidator: VehicleStatusValidator,
    private val vehicleConflictValidator: VehicleConflictValidator,
    private val childrenStatusValidator: ChildrenStatusValidator,
    private val vehicleCapacityValidator: VehicleCapacityValidator,
    private val scheduleOwnershipValidator: ScheduleOwnershipValidator
) {
    /**
     * Validates the CreateRoute command and returns the validation context.
     * The context contains pre-loaded data that the handler can reuse.
     *
     * @throws IllegalArgumentException if any validation fails
     * @throws DriverNotFoundException if driver not found
     * @throws VehicleNotFoundException if vehicle not found
     * @throws ChildNotFoundException if any child not found
     * @return CreateRouteValidationContext with pre-loaded aggregates
     */
    suspend fun validate(command: CreateRouteCommand): CreateRouteValidationContext {
        // Step 1: Validate stop orders (doesn't need context)
        stopOrderValidator.validate(command)

        // Step 2: Build validation context (batch load all data)
        val context = contextBuilder.build(command)

        // Step 3: Run all validators that need context
        driverStatusValidator.validate(context)
        driverConflictValidator.validate(command, context)

        vehicleStatusValidator.validate(context)
        vehicleConflictValidator.validate(command, context)

        childrenStatusValidator.validate(context)
        scheduleOwnershipValidator.validate(command, context)
        vehicleCapacityValidator.validate(command, context)

        // Step 4: Return context for handler to use
        return context
    }
}