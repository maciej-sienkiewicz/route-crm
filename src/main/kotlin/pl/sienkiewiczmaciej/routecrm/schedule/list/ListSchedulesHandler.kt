package pl.sienkiewiczmaciej.routecrm.schedule.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class ListSchedulesQuery(
    val companyId: CompanyId,
    val childId: ChildId
)

data class ScheduleListItem(
    val id: ScheduleId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val dropoffTime: LocalTime,
    val active: Boolean
)

@Component
class ListSchedulesHandler(
    private val scheduleRepository: ScheduleRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListSchedulesQuery): List<ScheduleListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val schedules = scheduleRepository.findByChild(query.companyId, query.childId)

        return schedules.map { schedule ->
            ScheduleListItem(
                id = schedule.id,
                name = schedule.name,
                days = schedule.days,
                pickupTime = schedule.pickupTime,
                dropoffTime = schedule.dropoffTime,
                active = schedule.active
            )
        }
    }
}