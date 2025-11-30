package pl.sienkiewiczmaciej.routecrm.driverauth.unlock

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driverauth.service.DriverAuthService
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

@Component
class UnlockAccountHandler(
    private val driverAuthService: DriverAuthService,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UnlockAccountCommand) {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        driverAuthService.unlockAccount(command.companyId, command.driverId)
    }
}