package pl.sienkiewiczmaciej.routecrm.test

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole

class TestAuthenticationFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val userId = request.getHeader("X-Test-User-Id")
        val companyId = request.getHeader("X-Test-Company-Id")
        val userRole = request.getHeader("X-Test-User-Role")

        if (userId != null && companyId != null && userRole != null) {
            val principal = UserPrincipal(
                userId = UserId.from(userId),
                companyId = CompanyId.from(companyId),
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                role = UserRole.valueOf(userRole),
                guardianId = request.getHeader("X-Test-Guardian-Id"),
                driverId = request.getHeader("X-Test-Driver-Id")
            )

            val authorities = listOf(SimpleGrantedAuthority("ROLE_$userRole"))
            val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}

