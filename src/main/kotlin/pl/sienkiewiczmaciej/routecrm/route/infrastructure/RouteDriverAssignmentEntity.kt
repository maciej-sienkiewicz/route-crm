// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteDriverAssignmentEntity.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import jakarta.persistence.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteDriverAssignment
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteDriverAssignmentId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import java.time.Instant

@Entity
@Table(
    name = "route_driver_assignments",
    indexes = [
        Index(name = "idx_assignments_route", columnList = "company_id, route_id, reassigned_at"),
        Index(name = "idx_assignments_date_range", columnList = "company_id, reassigned_at"),
        Index(name = "idx_assignments_driver", columnList = "company_id, new_driver_id, reassigned_at")
    ]
)
class RouteDriverAssignmentEntity(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(name = "company_id", nullable = false, length = 50)
    val companyId: String,

    @Column(name = "route_id", nullable = false, length = 50)
    val routeId: String,

    @Column(name = "previous_driver_id", nullable = true, length = 50)
    val previousDriverId: String?,

    @Column(name = "new_driver_id", nullable = false, length = 50)
    val newDriverId: String,

    @Column(name = "reassigned_by", nullable = false, length = 50)
    val reassignedBy: String,

    @Column(name = "reassigned_at", nullable = false)
    val reassignedAt: Instant = Instant.now(),

    @Column(columnDefinition = "text")
    val reason: String?
) {
    fun toDomain() = RouteDriverAssignment(
        id = RouteDriverAssignmentId(id),
        companyId = CompanyId(companyId),
        routeId = RouteId(routeId),
        previousDriverId = previousDriverId?.let { DriverId(it) } ,
        newDriverId = DriverId(newDriverId),
        reassignedBy = UserId(reassignedBy),
        reassignedAt = reassignedAt,
        reason = reason
    )

    companion object {
        fun fromDomain(assignment: RouteDriverAssignment) = RouteDriverAssignmentEntity(
            id = assignment.id.value,
            companyId = assignment.companyId.value,
            routeId = assignment.routeId.value,
            previousDriverId = assignment.previousDriverId?.value,
            newDriverId = assignment.newDriverId.value,
            reassignedBy = assignment.reassignedBy.value,
            reassignedAt = assignment.reassignedAt,
            reason = assignment.reason
        )
    }
}