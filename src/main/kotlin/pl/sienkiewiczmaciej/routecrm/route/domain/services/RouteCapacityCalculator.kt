// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/services/RouteCapacityCalculator.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleCapacity

/**
 * Value object representing capacity requirements for a route.
 */
data class CapacityRequirements(
    val totalSeats: Int,
    val wheelchairSpaces: Int,
    val childSeats: Int
) {
    init {
        require(totalSeats >= 0) { "Total seats cannot be negative" }
        require(wheelchairSpaces >= 0) { "Wheelchair spaces cannot be negative" }
        require(childSeats >= 0) { "Child seats cannot be negative" }
        require(wheelchairSpaces <= totalSeats) { "Wheelchair spaces cannot exceed total seats" }
        require(childSeats <= totalSeats) { "Child seats cannot exceed total seats" }
    }
}

/**
 * Result of a capacity check operation.
 */
sealed class CapacityCheckResult {
    object Fits : CapacityCheckResult()

    data class InsufficientWheelchairSpaces(
        val required: Int,
        val available: Int
    ) : CapacityCheckResult()

    data class InsufficientChildSeats(
        val required: Int,
        val available: Int
    ) : CapacityCheckResult()

    data class InsufficientTotalSeats(
        val required: Int,
        val available: Int
    ) : CapacityCheckResult()
}

/**
 * Domain service for calculating and validating vehicle capacity requirements.
 * Contains pure business logic for capacity calculations.
 */
@Component
class RouteCapacityCalculator {

    /**
     * Calculates capacity requirements based on a list of children.
     * Counts unique children and their specific needs.
     *
     * @param children List of children to calculate requirements for
     * @return CapacityRequirements with calculated needs
     */
    fun calculateRequirements(children: List<Child>): CapacityRequirements {
        val uniqueChildren = children.distinctBy { it.id }

        return CapacityRequirements(
            totalSeats = uniqueChildren.size,
            wheelchairSpaces = uniqueChildren.count { it.transportNeeds.wheelchair },
            childSeats = uniqueChildren.count { it.transportNeeds.specialSeat }
        )
    }

    /**
     * Checks if the capacity requirements fit within the vehicle capacity.
     * Returns the first capacity violation found, or Fits if all requirements are met.
     *
     * @param requirements The capacity requirements to check
     * @param vehicleCapacity The available vehicle capacity
     * @return CapacityCheckResult indicating if requirements fit or what's insufficient
     */
    fun checkFits(
        requirements: CapacityRequirements,
        vehicleCapacity: VehicleCapacity
    ): CapacityCheckResult {
        // Check wheelchair spaces first (most specific)
        if (requirements.wheelchairSpaces > vehicleCapacity.wheelchairSpaces) {
            return CapacityCheckResult.InsufficientWheelchairSpaces(
                required = requirements.wheelchairSpaces,
                available = vehicleCapacity.wheelchairSpaces
            )
        }

        // Check child seats (specific)
        if (requirements.childSeats > vehicleCapacity.childSeats) {
            return CapacityCheckResult.InsufficientChildSeats(
                required = requirements.childSeats,
                available = vehicleCapacity.childSeats
            )
        }

        // Check total seats (most general)
        if (requirements.totalSeats > vehicleCapacity.totalSeats) {
            return CapacityCheckResult.InsufficientTotalSeats(
                required = requirements.totalSeats,
                available = vehicleCapacity.totalSeats
            )
        }

        return CapacityCheckResult.Fits
    }

    /**
     * Convenience method that both calculates requirements and checks if they fit.
     *
     * @param children List of children to transport
     * @param vehicleCapacity The available vehicle capacity
     * @return CapacityCheckResult indicating if children fit in the vehicle
     */
    fun checkChildrenFit(
        children: List<Child>,
        vehicleCapacity: VehicleCapacity
    ): CapacityCheckResult {
        val requirements = calculateRequirements(children)
        return checkFits(requirements, vehicleCapacity)
    }
}