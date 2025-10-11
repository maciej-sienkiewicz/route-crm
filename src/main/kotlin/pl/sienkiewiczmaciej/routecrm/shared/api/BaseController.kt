package pl.sienkiewiczmaciej.routecrm.shared.api

import org.springframework.security.core.context.SecurityContextHolder
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.UnauthorizedException

abstract class BaseController {

    protected fun getPrincipal(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal

        if (principal is UserPrincipal) {
            return principal
        }

        throw UnauthorizedException("Invalid authentication principal")
    }
}