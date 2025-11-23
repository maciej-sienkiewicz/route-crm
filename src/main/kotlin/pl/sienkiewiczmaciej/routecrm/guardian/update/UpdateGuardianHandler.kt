package pl.sienkiewiczmaciej.routecrm.guardian.update

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class UpdateGuardianCommand(
    val companyId: CompanyId,
    val id: GuardianId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val alternatePhone: String?,
    val address: Address,
)

data class UpdateGuardianResult(
    val id: GuardianId,
    val firstName: String,
    val lastName: String,
    val email: String?
)

@Component
class UpdateGuardianHandler(
    private val guardianRepository: GuardianRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateGuardianCommand): UpdateGuardianResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val guardian = guardianRepository.findById(command.companyId, command.id)
            ?: throw GuardianNotFoundException(command.id)

        if (guardianRepository.existsByEmailExcludingId(command.companyId, command.email, command.id)) {
            throw IllegalArgumentException("Guardian with email ${command.email} already exists")
        }

        val updated = guardian.update(
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            address = command.address,
        )

        val saved = guardianRepository.save(updated)

        return UpdateGuardianResult(
            id = saved.id,
            firstName = saved.firstName,
            lastName = saved.lastName,
            email = saved?.email
        )
    }
}