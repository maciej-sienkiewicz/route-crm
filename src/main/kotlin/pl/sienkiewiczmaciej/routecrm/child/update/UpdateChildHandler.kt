package pl.sienkiewiczmaciej.routecrm.child.update

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.*
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class UpdateChildCommand(
    val companyId: CompanyId,
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val status: ChildStatus,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeeds,
    val notes: String?
)

data class UpdateChildResult(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val status: ChildStatus
)

@Component
class UpdateChildHandler(
    private val childRepository: ChildRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateChildCommand): UpdateChildResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val child = childRepository.findById(command.companyId, command.id)
            ?: throw ChildNotFoundException(command.id)

        val updated = child.update(
            firstName = command.firstName,
            lastName = command.lastName,
            birthDate = command.birthDate,
            status = command.status,
            disability = command.disability,
            transportNeeds = command.transportNeeds,
            notes = command.notes
        )

        val saved = childRepository.save(updated)

        return UpdateChildResult(
            id = saved.id,
            firstName = saved.firstName,
            lastName = saved.lastName,
            status = saved.status
        )
    }
}