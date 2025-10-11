package pl.sienkiewiczmaciej.routecrm.child.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.*
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class ListChildrenQuery(
    val companyId: CompanyId,
    val status: ChildStatus?,
    val pageable: Pageable,
    val guardianId: String?
)

data class ChildListItem(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val age: Int,
    val status: ChildStatus,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeeds,
    val guardiansCount: Int,
    val activeSchedulesCount: Int
)

@Component
class ListChildrenHandler(
    private val childRepository: ChildRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListChildrenQuery): Page<ChildListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val children = if (principal.role == UserRole.GUARDIAN && query.guardianId != null) {
            childRepository.findByGuardian(
                companyId = query.companyId,
                guardianId = pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId.from(query.guardianId),
                pageable = query.pageable
            )
        } else {
            childRepository.findAll(
                companyId = query.companyId,
                status = query.status,
                pageable = query.pageable
            )
        }

        return children.map { child ->
            val guardiansCount = guardianAssignmentRepository.countByCompanyIdAndChildId(
                query.companyId.value,
                child.id.value
            ).toInt()

            ChildListItem(
                id = child.id,
                firstName = child.firstName,
                lastName = child.lastName,
                birthDate = child.birthDate,
                age = child.age(),
                status = child.status,
                disability = child.disability,
                transportNeeds = child.transportNeeds,
                guardiansCount = guardiansCount,
                activeSchedulesCount = 0
            )
        }
    }
}