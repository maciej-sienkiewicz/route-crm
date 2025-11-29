package pl.sienkiewiczmaciej.routecrm.guardian.children

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianRelationship
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class GuardianAssignment(
    val companyId: CompanyId,
    val guardianId: GuardianId,
    val childId: ChildId,
    val relationship: GuardianRelationship,
    val isPrimary: Boolean,
    val canPickup: Boolean,
    val canAuthorize: Boolean
)

interface GuardianAssignmentRepository {
    suspend fun save(assignment: GuardianAssignment): GuardianAssignment
    suspend fun findByGuardianAndChild(
        companyId: CompanyId,
        guardianId: GuardianId,
        childId: ChildId
    ): GuardianAssignment?
    suspend fun findByChild(companyId: CompanyId, childId: ChildId): List<GuardianAssignment>
    suspend fun findByGuardian(companyId: CompanyId, guardianId: GuardianId): List<GuardianAssignment>
    suspend fun delete(companyId: CompanyId, guardianId: GuardianId, childId: ChildId)
    suspend fun countByGuardian(companyId: CompanyId, guardianId: GuardianId): Long
    suspend fun countByChild(companyId: CompanyId, childId: ChildId): Long
}