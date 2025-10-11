package pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole

@Component
class AuthorizationService {

    fun requireRole(principal: UserPrincipal, vararg allowedRoles: UserRole) {
        if (principal.role !in allowedRoles) {
            throw ForbiddenException("Access denied for role ${principal.role}")
        }
    }

    fun requireSameCompany(principalCompanyId: CompanyId, resourceCompanyId: CompanyId) {
        if (principalCompanyId != resourceCompanyId) {
            throw ForbiddenException("Access denied to company ${resourceCompanyId.value}")
        }
    }
}

class UnauthorizedException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)