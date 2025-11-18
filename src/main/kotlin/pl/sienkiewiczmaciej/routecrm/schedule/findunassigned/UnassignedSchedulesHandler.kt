package pl.sienkiewiczmaciej.routecrm.schedule.findunassigned

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate
import java.time.LocalTime

/**
 * Query to fetch unassigned schedules for a specific date
 */
data class ListUnassignedSchedulesQuery(
    val companyId: CompanyId,
    val date: LocalDate
)

/**
 * Single unassigned schedule item with enriched child information
 */
data class UnassignedScheduleItem(
    val scheduleId: ScheduleId,
    val childId: ChildId,
    val childFirstName: String,
    val childLastName: String,
    val scheduleName: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddress,
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddress,
    val specialInstructions: String?
)

/**
 * Result containing all unassigned schedules for the requested date
 */
data class UnassignedSchedulesResult(
    val date: LocalDate,
    val schedules: List<UnassignedScheduleItem>
) {
    val totalCount: Int
        get() = schedules.size
}

/**
 * Handler for listing schedules that are not assigned to any route on a given date
 * and are not covered by any absence.
 *
 * This is primarily used by operators during route planning to see which children
 * still need transportation arranged.
 */
@Component
class ListUnassignedSchedulesHandler(
    private val scheduleRepository: ScheduleRepository,
    private val childRepository: ChildRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(
        principal: UserPrincipal,
        query: ListUnassignedSchedulesQuery
    ): UnassignedSchedulesResult {
        // Only ADMIN and OPERATOR can view unassigned schedules for route planning
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val unassignedSchedules = scheduleRepository.findUnassignedForDate(
            query.companyId,
            query.date
        )

        // Enrich schedules with child information
        val items = unassignedSchedules.map { schedule ->
            val child = childRepository.findById(query.companyId, schedule.childId)
                ?: error("Child ${schedule.childId.value} not found for schedule ${schedule.id.value}")

            UnassignedScheduleItem(
                scheduleId = schedule.id,
                childId = schedule.childId,
                childFirstName = child.firstName,
                childLastName = child.lastName,
                scheduleName = schedule.name,
                days = schedule.days,
                pickupTime = schedule.pickupTime,
                pickupAddress = schedule.pickupAddress,
                dropoffTime = schedule.dropoffTime,
                dropoffAddress = schedule.dropoffAddress,
                specialInstructions = schedule.specialInstructions
            )
        }

        return UnassignedSchedulesResult(
            date = query.date,
            schedules = items
        )
    }
}