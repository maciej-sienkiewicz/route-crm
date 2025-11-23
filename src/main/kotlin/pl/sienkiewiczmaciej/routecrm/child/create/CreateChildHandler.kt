package pl.sienkiewiczmaciej.routecrm.child.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.*
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GuardianNotFoundException
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentEntity
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianRelationship
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class NewGuardianData(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val relationship: GuardianRelationship
)

data class CreateChildCommand(
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeeds,
    val notes: String?,
    val guardianId: GuardianId?,
    val newGuardianData: NewGuardianData?
)

data class CreateChildResult(
    val childId: ChildId,
    val guardianId: GuardianId,
    val child: Child
)

@Component
class CreateChildHandler(
    private val childRepository: ChildRepository,
    private val guardianRepository: GuardianRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateChildCommand): CreateChildResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        require(command.firstName.isNotBlank()) { "First name is required" }
        require(command.lastName.isNotBlank()) { "Last name is required" }
        require(command.birthDate.isBefore(LocalDate.now())) { "Birth date must be in the past" }
        require(command.disability.isNotEmpty()) { "At least one disability type is required" }

        val guardian = if (command.guardianId != null) {
            guardianRepository.findById(command.companyId, command.guardianId)
                ?: throw GuardianNotFoundException(command.guardianId)
        } else {
            require(command.newGuardianData != null) { "Guardian information is required" }
            val newGuardian = Guardian.create(
                companyId = command.companyId,
                firstName = command.newGuardianData.firstName,
                lastName = command.newGuardianData.lastName,
                email = command.newGuardianData.email,
                phone = command.newGuardianData.phone,
                address = Address(
                    street = "ul. Przykładowa",
                    houseNumber = "1",
                    apartmentNumber = null,
                    postalCode = "00-001",
                    city = "Warszawa"
                ),
            )
            guardianRepository.save(newGuardian)
        }

        val child = Child.create(
            companyId = command.companyId,
            firstName = command.firstName,
            lastName = command.lastName,
            birthDate = command.birthDate,
            disability = command.disability,
            transportNeeds = command.transportNeeds,
            notes = command.notes
        )

        val savedChild = childRepository.save(child)

        val assignment = GuardianAssignmentEntity(
            companyId = command.companyId.value,
            guardianId = guardian.id.value,
            childId = savedChild.id.value,
            relationship = command.newGuardianData?.relationship ?: GuardianRelationship.PARENT,
            isPrimary = true,
            canPickup = true,
            canAuthorize = true
        )
        guardianAssignmentRepository.save(assignment)

        return CreateChildResult(
            childId = savedChild.id,
            guardianId = guardian.id,
            child = savedChild
        )
    }
}