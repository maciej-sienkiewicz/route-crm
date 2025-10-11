package pl.sienkiewiczmaciej.routecrm.guardian.list

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.CommunicationPreference
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class ListGuardiansQuery(
    val companyId: CompanyId,
    val search: String?,
    val pageable: Pageable
)

data class GuardianListItem(
    val id: GuardianId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val childrenCount: Int = 0
)

@Component
class ListGuardiansHandler(
    private val guardianRepository: GuardianRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListGuardiansQuery): Page<GuardianListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val guardians = guardianRepository.findAll(
            companyId = query.companyId,
            search = query.search,
            pageable = query.pageable
        )

        return guardians.map { guardian ->
            GuardianListItem(
                id = guardian.id,
                firstName = guardian.firstName,
                lastName = guardian.lastName,
                email = guardian.email,
                phone = guardian.phone,
                childrenCount = 0
            )
        }
    }
}