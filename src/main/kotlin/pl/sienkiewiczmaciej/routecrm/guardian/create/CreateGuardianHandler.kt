package pl.sienkiewiczmaciej.routecrm.guardian.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.domain.CommunicationPreference
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService

data class CreateGuardianCommand(
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val alternatePhone: String?,
    val address: Address,
    val communicationPreference: CommunicationPreference
)

data class CreateGuardianResult(
    val id: GuardianId,
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val alternatePhone: String?,
    val address: Address,
    val communicationPreference: CommunicationPreference
)

@Component
class CreateGuardianHandler(
    private val guardianRepository: GuardianRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateGuardianCommand): CreateGuardianResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        if (guardianRepository.existsByEmail(command.companyId, command.email)) {
            throw IllegalArgumentException("Guardian with email ${command.email} already exists")
        }

        val guardian = Guardian.create(
            companyId = command.companyId,
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            alternatePhone = command.alternatePhone,
            address = command.address,
            communicationPreference = command.communicationPreference
        )

        val saved = guardianRepository.save(guardian)

        return CreateGuardianResult(
            id = saved.id,
            companyId = saved.companyId,
            firstName = saved.firstName,
            lastName = saved.lastName,
            email = saved.email,
            phone = saved.phone,
            alternatePhone = saved.alternatePhone,
            address = saved.address,
            communicationPreference = saved.communicationPreference
        )
    }
}