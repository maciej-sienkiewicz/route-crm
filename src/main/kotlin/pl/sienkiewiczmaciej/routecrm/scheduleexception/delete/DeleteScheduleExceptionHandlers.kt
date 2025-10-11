package pl.sienkiewiczmaciej.routecrm.scheduleexception.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteScheduleExceptionCommand(
    val companyId: CompanyId,
    val id: ScheduleExceptionId
)

class ScheduleExceptionNotFoundException(id: ScheduleExceptionId) :
    NotFoundException("Schedule exception ${id.value} not found")

@Component
class DeleteScheduleExceptionHandler(
    private val scheduleExceptionRepository: ScheduleExceptionRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteScheduleExceptionCommand) {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val exception = scheduleExceptionRepository.findById(command.companyId, command.id)
            ?: throw ScheduleExceptionNotFoundException(command.id)

        scheduleExceptionRepository.delete(command.companyId, command.id)
    }
}