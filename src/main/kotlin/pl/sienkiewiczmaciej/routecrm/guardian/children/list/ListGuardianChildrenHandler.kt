package pl.sienkiewiczmaciej.routecrm.guardian.children.list

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.guardian.children.GuardianAssignmentRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianRelationship
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate
import java.time.Period

data class ListGuardianChildrenQuery(
    val companyId: CompanyId,
    val guardianId: GuardianId
)

data class GuardianChildItem(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val relationship: GuardianRelationship,
    val isPrimary: Boolean,
    val status: String
)

@Component
class ListGuardianChildrenHandler(
    private val assignmentRepository: GuardianAssignmentRepository,
    private val childRepository: ChildRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListGuardianChildrenQuery): List<GuardianChildItem> = coroutineScope {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val assignments = assignmentRepository.findByGuardian(query.companyId, query.guardianId)

        val children = assignments.map { assignment ->
            async {
                val child = childRepository.findById(query.companyId, assignment.childId)
                child?.let {
                    val age = Period.between(it.birthDate, LocalDate.now()).years
                    GuardianChildItem(
                        id = it.id,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        age = age,
                        relationship = assignment.relationship,
                        isPrimary = assignment.isPrimary,
                        status = it.status.name
                    )
                }
            }
        }.mapNotNull { it.await() }

        children
    }
}