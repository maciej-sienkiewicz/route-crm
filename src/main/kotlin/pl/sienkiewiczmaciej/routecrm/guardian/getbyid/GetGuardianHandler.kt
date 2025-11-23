package pl.sienkiewiczmaciej.routecrm.guardian.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class GetGuardianQuery(
    val companyId: CompanyId,
    val id: GuardianId
)

data class GuardianDetail(
    val id: GuardianId,
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String,
    val address: Address?,
    val children: List<GuardianChildInfo> = emptyList()
)

data class GuardianChildInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val relationship: String,
    val isPrimary: Boolean
)

class GuardianNotFoundException(id: GuardianId) : NotFoundException("Guardian ${id.value} not found")

@Component
class GetGuardianHandler(
    private val guardianRepository: GuardianRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetGuardianQuery): GuardianDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val guardian = guardianRepository.findById(query.companyId, query.id)
            ?: throw GuardianNotFoundException(query.id)

        return GuardianDetail(
            id = guardian.id,
            companyId = guardian.companyId,
            firstName = guardian.firstName,
            lastName = guardian.lastName,
            email = guardian.email,
            phone = guardian.phone,
            address = guardian.address,
            children = emptyList()
        )
    }
}