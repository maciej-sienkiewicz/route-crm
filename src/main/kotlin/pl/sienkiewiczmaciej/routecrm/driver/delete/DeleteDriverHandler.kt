package pl.sienkiewiczmaciej.routecrm.driver.delete

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class DeleteDriverCommand(
    val companyId: CompanyId,
    val id: DriverId
)

@Component
class DeleteDriverHandler(
    private val driverRepository: DriverRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: DeleteDriverCommand) {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val driver = driverRepository.findById(command.companyId, command.id)
            ?: throw DriverNotFoundException(command.id)

        val deactivated = driver.copy(status = DriverStatus.INACTIVE)
        driverRepository.save(deactivated)
    }
}