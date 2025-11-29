package pl.sienkiewiczmaciej.routecrm.guardian.getbyid

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.guardian.children.GuardianAssignment
import pl.sienkiewiczmaciej.routecrm.guardian.children.GuardianAssignmentRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository

data class GetGuardianValidationContext(
    val guardian: Guardian,
    val assignments: List<GuardianAssignment>,
    val children: List<Child>
)

@Component
class GetGuardianContextBuilder(
    private val guardianRepository: GuardianRepository,
    private val assignmentRepository: GuardianAssignmentRepository,
    private val childRepository: ChildRepository
) {
    suspend fun build(query: GetGuardianQuery): GetGuardianValidationContext = coroutineScope {
        val guardianDeferred = async {
            guardianRepository.findById(query.companyId, query.id)
                ?: throw GuardianNotFoundException(query.id)
        }

        val guardian = guardianDeferred.await()

        val assignmentsDeferred = async {
            assignmentRepository.findByGuardian(query.companyId, query.id)
        }

        val assignments = assignmentsDeferred.await()

        val childrenDeferred = assignments.map { assignment ->
            async {
                childRepository.findById(query.companyId, assignment.childId)
            }
        }

        val children = childrenDeferred.mapNotNull { it.await() }

        GetGuardianValidationContext(
            guardian = guardian,
            assignments = assignments,
            children = children
        )
    }
}
