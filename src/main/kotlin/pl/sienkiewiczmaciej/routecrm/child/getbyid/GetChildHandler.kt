package pl.sienkiewiczmaciej.routecrm.child.getbyid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.*
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianRelationship
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class GetChildQuery(
    val companyId: CompanyId,
    val id: ChildId
)

data class ChildDetail(
    val id: ChildId,
    val companyId: CompanyId,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val age: Int,
    val status: ChildStatus,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeeds,
    val notes: String?,
    val guardians: List<ChildGuardianInfo>
)

data class ChildGuardianInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String,
    val relationship: GuardianRelationship,
    val isPrimary: Boolean,
    val canPickup: Boolean,
    val canAuthorize: Boolean
)

class ChildNotFoundException(id: ChildId) : NotFoundException("Child ${id.value} not found")

@Component
class GetChildHandler(
    private val childRepository: ChildRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val guardianRepository: GuardianJpaRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetChildQuery): ChildDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val child = childRepository.findById(query.companyId, query.id)
            ?: throw ChildNotFoundException(query.id)

        val guardians = withContext(Dispatchers.IO) {
            val assignments = guardianAssignmentRepository.findByCompanyIdAndChildId(
                query.companyId.value,
                query.id.value
            )

            assignments.mapNotNull { assignment ->
                val guardian = guardianRepository.findByIdAndCompanyId(
                    assignment.guardianId,
                    query.companyId.value
                )
                guardian?.let {
                    ChildGuardianInfo(
                        id = it.id,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        email = it.email,
                        phone = it.phone,
                        relationship = assignment.relationship,
                        isPrimary = assignment.isPrimary,
                        canPickup = assignment.canPickup,
                        canAuthorize = assignment.canAuthorize
                    )
                }
            }
        }

        return ChildDetail(
            id = child.id,
            companyId = child.companyId,
            firstName = child.firstName,
            lastName = child.lastName,
            birthDate = child.birthDate,
            age = child.age(),
            status = child.status,
            disability = child.disability,
            transportNeeds = child.transportNeeds,
            notes = child.notes,
            guardians = guardians
        )
    }
}