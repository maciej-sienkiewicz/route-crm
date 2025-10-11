package pl.sienkiewiczmaciej.routecrm.test.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole

@TestConfiguration
@EnableWebSecurity
@Profile("test")
class TestSecurityConfig {

    @Bean
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .addFilterBefore(TestAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}

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
                userId = UserId(userId),
                companyId = CompanyId(companyId),
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                role = UserRole.valueOf(userRole),
                guardianId = request.getHeader("X-Test-Guardian-Id"),
                driverId = request.getHeader("X-Test-Driver-Id")
            )

            val authentication = UsernamePasswordAuthenticationToken(
                principal,
                null,
                emptyList()
            )

            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}