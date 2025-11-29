// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/guardian/getbyid/GetGuardianHandler.kt
package pl.sienkiewiczmaciej.routecrm.guardian.getbyid

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.services.GuardianAccountInfo
import pl.sienkiewiczmaciej.routecrm.guardian.services.GuardianAccountService
import pl.sienkiewiczmaciej.routecrm.guardian.services.GuardianChild
import pl.sienkiewiczmaciej.routecrm.guardian.services.GuardianChildrenService
import pl.sienkiewiczmaciej.routecrm.guardian.services.GuardianStats
import pl.sienkiewiczmaciej.routecrm.guardian.services.GuardianStatsService
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
    val children: List<GuardianChild>,
    val accountInfo: GuardianAccountInfo,
    val stats: GuardianStats
)

class GuardianNotFoundException(id: GuardianId) : NotFoundException("Guardian ${id.value} not found")

@Component
class GetGuardianHandler(
    private val guardianRepository: GuardianRepository,
    private val childrenService: GuardianChildrenService,
    private val statsService: GuardianStatsService,
    private val accountService: GuardianAccountService,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetGuardianQuery): GuardianDetail = coroutineScope {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val guardian = guardianRepository.findById(query.companyId, query.id)
            ?: throw GuardianNotFoundException(query.id)

        val childrenDeferred = async {
            childrenService.getChildren(query.companyId, query.id)
        }

        val accountInfoDeferred = async {
            accountService.getAccountInfo(query.companyId, query.id, guardian)
        }

        val children = childrenDeferred.await()
        val accountInfo = accountInfoDeferred.await()

        val stats = statsService.calculateStats(query.companyId, query.id, children)

        GuardianDetail(
            id = guardian.id,
            companyId = guardian.companyId,
            firstName = guardian.firstName,
            lastName = guardian.lastName,
            email = guardian.email,
            phone = guardian.phone,
            address = guardian.address,
            children = children,
            accountInfo = accountInfo,
            stats = stats
        )
    }
}