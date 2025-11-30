package pl.sienkiewiczmaciej.routecrm.driverauth.resetpassword

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driverauth.service.DriverAuthService
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

@Component
class DriverResetPasswordHandler(
    private val driverAuthService: DriverAuthService,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: ResetPasswordCommand): ResetPasswordResult {
        authService.requireRole(principal, UserRole.ADMIN)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val newPin = driverAuthService.resetPassword(command.companyId, command.driverId)

        return ResetPasswordResult(newPin)
    }
}