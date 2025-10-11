package pl.sienkiewiczmaciej.routecrm.schedule.update

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.schedule.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.ScheduleNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class UpdateScheduleCommand(
    val companyId: CompanyId,
    val id: ScheduleId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddress,
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddress,
    val specialInstructions: String?,
    val active: Boolean
)

data class UpdateScheduleResult(
    val id: ScheduleId,
    val name: String
)

@Component
class UpdateScheduleHandler(
    private val scheduleRepository: ScheduleRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateScheduleCommand): UpdateScheduleResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val schedule = scheduleRepository.findById(command.companyId, command.id)
            ?: throw ScheduleNotFoundException(command.id)

        val updated = schedule.update(
            name = command.name,
            days = command.days,
            pickupTime = command.pickupTime,
            pickupAddress = command.pickupAddress,
            dropoffTime = command.dropoffTime,
            dropoffAddress = command.dropoffAddress,
            specialInstructions = command.specialInstructions,
            active = command.active
        )

        val saved = scheduleRepository.save(updated)

        return UpdateScheduleResult(
            id = saved.id,
            name = saved.name
        )
    }
}