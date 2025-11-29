package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.guardian.children.GuardianAssignment
import pl.sienkiewiczmaciej.routecrm.guardian.children.GuardianAssignmentRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class GuardianAssignmentRepositoryImpl(
    private val jpaRepository: GuardianAssignmentJpaRepository
) : GuardianAssignmentRepository {

    override suspend fun save(assignment: GuardianAssignment): GuardianAssignment =
        withContext(Dispatchers.IO) {
            val entity = GuardianAssignmentEntity(
                companyId = assignment.companyId.value,
                guardianId = assignment.guardianId.value,
                childId = assignment.childId.value,
                relationship = assignment.relationship,
                isPrimary = assignment.isPrimary,
                canPickup = assignment.canPickup,
                canAuthorize = assignment.canAuthorize
            )
            jpaRepository.save(entity)
            assignment
        }

    override suspend fun findByGuardianAndChild(
        companyId: CompanyId,
        guardianId: GuardianId,
        childId: ChildId
    ): GuardianAssignment? = withContext(Dispatchers.IO) {
        jpaRepository.findByCompanyIdAndGuardianIdAndChildId(
            companyId.value,
            guardianId.value,
            childId.value
        )?.let {
            GuardianAssignment(
                companyId = CompanyId(it.companyId),
                guardianId = GuardianId(it.guardianId),
                childId = ChildId(it.childId),
                relationship = it.relationship,
                isPrimary = it.isPrimary,
                canPickup = it.canPickup,
                canAuthorize = it.canAuthorize
            )
        }
    }

    override suspend fun findByChild(companyId: CompanyId, childId: ChildId): List<GuardianAssignment> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndChildId(companyId.value, childId.value).map {
                GuardianAssignment(
                    companyId = CompanyId(it.companyId),
                    guardianId = GuardianId(it.guardianId),
                    childId = ChildId(it.childId),
                    relationship = it.relationship,
                    isPrimary = it.isPrimary,
                    canPickup = it.canPickup,
                    canAuthorize = it.canAuthorize
                )
            }
        }

    override suspend fun findByGuardian(companyId: CompanyId, guardianId: GuardianId): List<GuardianAssignment> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndGuardianId(companyId.value, guardianId.value).map {
                GuardianAssignment(
                    companyId = CompanyId(it.companyId),
                    guardianId = GuardianId(it.guardianId),
                    childId = ChildId(it.childId),
                    relationship = it.relationship,
                    isPrimary = it.isPrimary,
                    canPickup = it.canPickup,
                    canAuthorize = it.canAuthorize
                )
            }
        }

    override suspend fun delete(companyId: CompanyId, guardianId: GuardianId, childId: ChildId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByCompanyIdAndGuardianIdAndChildId(
                companyId.value,
                guardianId.value,
                childId.value
            )
        }
    }

    override suspend fun countByGuardian(companyId: CompanyId, guardianId: GuardianId): Long =
        withContext(Dispatchers.IO) {
            jpaRepository.countByCompanyIdAndGuardianId(companyId.value, guardianId.value)
        }

    override suspend fun countByChild(companyId: CompanyId, childId: ChildId): Long =
        withContext(Dispatchers.IO) {
            jpaRepository.countByCompanyIdAndChildId(companyId.value, childId.value)
        }
}