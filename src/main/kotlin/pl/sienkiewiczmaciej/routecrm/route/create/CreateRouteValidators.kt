// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/create/CreateRouteValidators.kt
package pl.sienkiewiczmaciej.routecrm.route.create

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleStatus

/**
 * Validates that stop orders are consecutive starting from 1.
 */
@Component
class StopOrderValidator {
    fun validate(command: CreateRouteCommand) {
        require(command.stops.isNotEmpty()) {
            "At least one stop is required"
        }

        val stopOrders = command.stops.map { it.stopOrder }.sorted()
        require(stopOrders == (1..stopOrders.size).toList()) {
            "Stop orders must be consecutive starting from 1"
        }
    }
}

/**
 * Validates that driver has ACTIVE status.
 */
@Component
class DriverStatusValidator {
    fun validate(context: CreateRouteValidationContext) {
        require(context.driver.status == DriverStatus.ACTIVE) {
            "Driver must be ACTIVE to be assigned to a route"
        }
    }
}

/**
 * Validates that driver has no time conflicts on the same date.
 */
@Component
class DriverConflictValidator {
    fun validate(command: CreateRouteCommand, context: CreateRouteValidationContext) {
        val hasConflict = context.existingDriverRoutes.any { existingRoute ->
            // Check time overlap
            command.estimatedStartTime < existingRoute.estimatedEndTime &&
                    command.estimatedEndTime > existingRoute.estimatedStartTime
        }

        if (hasConflict) {
            throw IllegalArgumentException(
                "Driver ${command.driverId.value} already has a route at this time on ${command.date}"
            )
        }
    }
}

/**
 * Validates that vehicle has AVAILABLE status.
 */
@Component
class VehicleStatusValidator {
    fun validate(context: CreateRouteValidationContext) {
        require(context.vehicle.status == VehicleStatus.AVAILABLE) {
            "Vehicle must be AVAILABLE to be assigned to a route"
        }
    }
}

/**
 * Validates that vehicle has no time conflicts on the same date.
 */
@Component
class VehicleConflictValidator {
    fun validate(command: CreateRouteCommand, context: CreateRouteValidationContext) {
        val hasConflict = context.existingVehicleRoutes.any { existingRoute ->
            // Check time overlap
            command.estimatedStartTime < existingRoute.estimatedEndTime &&
                    command.estimatedEndTime > existingRoute.estimatedStartTime
        }

        if (hasConflict) {
            throw IllegalArgumentException(
                "Vehicle ${command.vehicleId.value} is already assigned to another route at this time on ${command.date}"
            )
        }
    }
}

/**
 * Validates that all children have ACTIVE status.
 */
@Component
class ChildrenStatusValidator {
    fun validate(context: CreateRouteValidationContext) {
        context.children.values.forEach { child ->
            require(child.status == ChildStatus.ACTIVE) {
                "Child ${child.id.value} must be ACTIVE to be assigned to a route"
            }
        }
    }
}

/**
 * Validates that vehicle capacity is sufficient for all children.
 * Checks wheelchair spaces, special seats, and total seats.
 */
@Component
class VehicleCapacityValidator {
    fun validate(command: CreateRouteCommand, context: CreateRouteValidationContext) {
        val vehicle = context.vehicle
        val childrenInStops = command.stops.map { context.children[it.childId]!! }

        // Count unique children requiring wheelchair
        val wheelchairCount = childrenInStops.distinctBy { it.id }.count { it.transportNeeds.wheelchair }
        require(wheelchairCount <= vehicle.capacity.wheelchairSpaces) {
            "Number of children requiring wheelchair ($wheelchairCount) exceeds vehicle wheelchair capacity (${vehicle.capacity.wheelchairSpaces})"
        }

        // Count unique children requiring special seats
        val specialSeatCount = childrenInStops.distinctBy { it.id }.count { it.transportNeeds.specialSeat }
        require(specialSeatCount <= vehicle.capacity.childSeats) {
            "Number of children requiring special seats ($specialSeatCount) exceeds vehicle special seat capacity (${vehicle.capacity.childSeats})"
        }

        // Count total unique children
        val uniqueChildrenCount = context.children.size
        require(uniqueChildrenCount <= vehicle.capacity.totalSeats) {
            "Number of children ($uniqueChildrenCount) exceeds vehicle capacity (${vehicle.capacity.totalSeats})"
        }
    }
}

/**
 * Validates that schedules belong to the correct children.
 */
@Component
class ScheduleOwnershipValidator {
    fun validate(command: CreateRouteCommand, context: CreateRouteValidationContext) {
        command.stops.forEach { stop ->
            val schedule = context.schedules[stop.scheduleId]
            requireNotNull(schedule) {
                "Schedule ${stop.scheduleId.value} not found"
            }

            require(schedule.childId == stop.childId) {
                "Schedule ${stop.scheduleId.value} does not belong to child ${stop.childId.value}"
            }
        }
    }
}