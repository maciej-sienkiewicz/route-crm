// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/RouteDriverAssignment.kt
package pl.sienkiewiczmaciej.routecrm.route.domain

import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant
import java.util.*

@JvmInline
value class RouteDriverAssignmentId(val value: String) {
    companion object {
        fun generate() = RouteDriverAssignmentId("RDA-${UUID.randomUUID()}")
        fun from(value: String) = RouteDriverAssignmentId(value)
    }
}

/**
 * Value Object representing a driver reassignment event.
 * Immutable - once created, cannot be changed.
 * This is an audit record, not part of the Route aggregate.
 */
data class RouteDriverAssignment(
    val id: RouteDriverAssignmentId,
    val companyId: CompanyId,
    val routeId: RouteId,
    val previousDriverId: DriverId?,
    val newDriverId: DriverId,
    val reassignedBy: UserId,
    val reassignedAt: Instant,
    val reason: String?
) {
    companion object {
        fun create(
            companyId: CompanyId,
            routeId: RouteId,
            previousDriverId: DriverId?,
            newDriverId: DriverId,
            reassignedBy: UserId,
            reason: String? = null
        ): RouteDriverAssignment {
            require(previousDriverId != newDriverId) {
                "Previous and new driver must be different"
            }
            require(reason == null || reason.length <= 5000) {
                "Reason cannot exceed 5000 characters"
            }

            return RouteDriverAssignment(
                id = RouteDriverAssignmentId.generate(),
                companyId = companyId,
                routeId = routeId,
                previousDriverId = previousDriverId,
                newDriverId = newDriverId,
                reassignedBy = reassignedBy,
                reassignedAt = Instant.now(),
                reason = reason?.trim()
            )
        }
    }
}