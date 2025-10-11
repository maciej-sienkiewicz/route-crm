package pl.sienkiewiczmaciej.routecrm.schedule.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class GetScheduleQuery(
    val companyId: CompanyId,
    val id: ScheduleId
)

data class ScheduleDetail(
    val id: ScheduleId,
    val companyId: CompanyId,
    val childId: ChildId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddress,
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddress,
    val specialInstructions: String?,
    val active: Boolean
)

class ScheduleNotFoundException(id: ScheduleId) : NotFoundException("Schedule ${id.value} not found")

@Component
class GetScheduleHandler(
    private val scheduleRepository: ScheduleRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetScheduleQuery): ScheduleDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val schedule = scheduleRepository.findById(query.companyId, query.id)
            ?: throw ScheduleNotFoundException(query.id)

        return ScheduleDetail(
            id = schedule.id,
            companyId = schedule.companyId,
            childId = schedule.childId,
            name = schedule.name,
            days = schedule.days,
            pickupTime = schedule.pickupTime,
            pickupAddress = schedule.pickupAddress,
            dropoffTime = schedule.dropoffTime,
            dropoffAddress = schedule.dropoffAddress,
            specialInstructions = schedule.specialInstructions,
            active = schedule.active
        )
    }
}