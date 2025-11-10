// route/availablechildren/ListAvailableChildrenHandler.kt (REFACTORED)
package pl.sienkiewiczmaciej.routecrm.route.availablechildren

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.child.domain.DisabilityType
import pl.sienkiewiczmaciej.routecrm.child.domain.TransportNeeds
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianJpaRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate
import java.time.LocalTime

data class ListAvailableChildrenQuery(
    val companyId: CompanyId,
    val date: LocalDate
)

data class AvailableChildItem(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val disability: Set<DisabilityType>,
    val transportNeeds: TransportNeeds,
    val scheduleId: ScheduleId,
    val scheduleName: String,
    val pickupTime: LocalTime,
    val dropoffTime: LocalTime,
    val pickupAddress: ScheduleAddress,
    val dropoffAddress: ScheduleAddress,
    val guardianFirstName: String,
    val guardianLastName: String,
    val guardianPhone: String
)

/**
 * Query handler for listing available children for a given date.
 * No validators needed - this is a read-only query.
 */
@Component
class ListAvailableChildrenHandler(
    private val childRepository: ChildJpaRepository,
    private val scheduleRepository: ScheduleJpaRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val guardianRepository: GuardianJpaRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListAvailableChildrenQuery): List<AvailableChildItem> {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        // 2. Convert date to day of week
        val dayOfWeek = convertToDayOfWeek(query.date)

        // 3. Load and filter data
        return withContext(Dispatchers.IO) {
            // Get all active children
            val activeChildren = childRepository.findByCompanyIdAndStatus(
                query.companyId.value,
                ChildStatus.ACTIVE,
                org.springframework.data.domain.Pageable.unpaged()
            ).content

            // Process each child in parallel
            val results = activeChildren.map { childEntity ->
                async {
                    processChild(childEntity, query.companyId.value, dayOfWeek)
                }
            }.awaitAll().flatten()

            // Sort results
            results.sortedWith(
                compareBy(
                    { it.pickupTime },
                    { it.lastName },
                    { it.firstName }
                )
            )
        }
    }

    private suspend fun processChild(
        childEntity: pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildEntity,
        companyIdValue: String,
        dayOfWeek: DayOfWeek
    ): List<AvailableChildItem> {
        val child = childEntity.toDomain()

        // Find active schedules for this child on this day
        val schedules = scheduleRepository.findByCompanyIdAndChildId(
            companyIdValue,
            child.id.value
        ).filter { it.active && it.days.contains(dayOfWeek) }

        // Get primary guardian
        val assignments = guardianAssignmentRepository.findByCompanyIdAndChildId(
            companyIdValue,
            child.id.value
        )
        val primaryAssignment = assignments.find { it.isPrimary } ?: assignments.firstOrNull()

        val (guardianFirstName, guardianLastName, guardianPhone) = if (primaryAssignment != null) {
            val guardian = guardianRepository.findByIdAndCompanyId(
                primaryAssignment.guardianId,
                companyIdValue
            )
            if (guardian != null) {
                Triple(guardian.firstName, guardian.lastName, guardian.phone)
            } else {
                Triple("", "", "")
            }
        } else {
            Triple("", "", "")
        }

        // Create item for each matching schedule
        return schedules.mapNotNull { scheduleEntity ->
            if (guardianFirstName.isNotEmpty()) {
                val schedule = scheduleEntity.toDomain()
                AvailableChildItem(
                    childId = child.id,
                    firstName = child.firstName,
                    lastName = child.lastName,
                    birthDate = child.birthDate,
                    disability = child.disability,
                    transportNeeds = child.transportNeeds,
                    scheduleId = schedule.id,
                    scheduleName = schedule.name,
                    pickupTime = schedule.pickupTime,
                    dropoffTime = schedule.dropoffTime,
                    pickupAddress = schedule.pickupAddress,
                    dropoffAddress = schedule.dropoffAddress,
                    guardianFirstName = guardianFirstName,
                    guardianLastName = guardianLastName,
                    guardianPhone = guardianPhone
                )
            } else {
                null
            }
        }
    }

    private fun convertToDayOfWeek(date: LocalDate): DayOfWeek {
        return when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
            java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }
}