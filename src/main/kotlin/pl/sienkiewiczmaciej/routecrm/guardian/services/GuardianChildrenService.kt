// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/guardian/services/GuardianChildrenService.kt
package pl.sienkiewiczmaciej.routecrm.guardian.services

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.guardian.children.GuardianAssignmentRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianRelationship
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate
import java.time.Period

data class GuardianChild(
    val child: Child,
    val relationship: GuardianRelationship,
    val isPrimary: Boolean
) {
    fun age(): Int = Period.between(child.birthDate, LocalDate.now()).years

    fun isActive(): Boolean = child.status == pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus.ACTIVE
}

@Service
class GuardianChildrenService(
    private val assignmentRepository: GuardianAssignmentRepository,
    private val childRepository: ChildRepository
) {
    @Transactional(readOnly = true)
    suspend fun getChildren(
        companyId: CompanyId,
        guardianId: GuardianId
    ): List<GuardianChild> = coroutineScope {
        val assignments = assignmentRepository.findByGuardian(companyId, guardianId)

        if (assignments.isEmpty()) {
            return@coroutineScope emptyList()
        }

        assignments.map { assignment ->
            async {
                childRepository.findById(companyId, assignment.childId)?.let { child ->
                    GuardianChild(
                        child = child,
                        relationship = assignment.relationship,
                        isPrimary = assignment.isPrimary
                    )
                }
            }
        }.mapNotNull { it.await() }
    }

    suspend fun countChildren(
        companyId: CompanyId,
        guardianId: GuardianId
    ): Long {
        return assignmentRepository.countByGuardian(companyId, guardianId)
    }

    suspend fun countActiveChildren(
        companyId: CompanyId,
        guardianId: GuardianId
    ): Int {
        val children = getChildren(companyId, guardianId)
        return children.count { it.isActive() }
    }
}