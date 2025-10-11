package pl.sienkiewiczmaciej.routecrm.schedule.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class CreateScheduleCommand(
    val companyId: CompanyId,
    val childId: ChildId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddress,
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddress,
    val specialInstructions: String?
)

data class CreateScheduleResult(
    val id: ScheduleId,
    val childId: ChildId,
    val companyId: CompanyId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val dropoffTime: LocalTime,
    val active: Boolean
)

@Component
class CreateScheduleHandler(
    private val scheduleRepository: ScheduleRepository,
    private val childRepository: ChildRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateScheduleCommand): CreateScheduleResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val child = childRepository.findById(command.companyId, command.childId)
            ?: throw ChildNotFoundException(command.childId)

        val schedule = Schedule.create(
            companyId = command.companyId,
            childId = command.childId,
            name = command.name,
            days = command.days,
            pickupTime = command.pickupTime,
            pickupAddress = command.pickupAddress,
            dropoffTime = command.dropoffTime,
            dropoffAddress = command.dropoffAddress,
            specialInstructions = command.specialInstructions
        )

        val saved = scheduleRepository.save(schedule)

        return CreateScheduleResult(
            id = saved.id,
            childId = saved.childId,
            companyId = saved.companyId,
            name = saved.name,
            days = saved.days,
            pickupTime = saved.pickupTime,
            dropoffTime = saved.dropoffTime,
            active = saved.active
        )
    }
}