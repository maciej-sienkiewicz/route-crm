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
import java.time.Period

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
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val dayOfWeek = convertToDayOfWeek(query.date)

        return withContext(Dispatchers.IO) {
            // Pobierz wszystkie aktywne dzieci dla firmy
            val activeChildren = childRepository.findByCompanyIdAndStatus(
                query.companyId.value,
                ChildStatus.ACTIVE,
                org.springframework.data.domain.Pageable.unpaged()
            ).content

            // Dla każdego dziecka znajdź aktywne harmonogramy dla danego dnia
            val results = activeChildren.map { childEntity ->
                async {
                    val child = childEntity.toDomain()

                    // Znajdź harmonogramy dla dziecka
                    val schedules = scheduleRepository.findByCompanyIdAndChildId(
                        query.companyId.value,
                        child.id.value
                    )

                    // Filtruj harmonogramy: aktywne i zawierające odpowiedni dzień tygodnia
                    val matchingSchedules = schedules.filter { scheduleEntity ->
                        scheduleEntity.active && scheduleEntity.days.contains(dayOfWeek)
                    }

                    // Dla każdego pasującego harmonogramu stwórz wpis
                    matchingSchedules.mapNotNull { scheduleEntity ->
                        val schedule = scheduleEntity.toDomain()

                        // Pobierz opiekuna
                        val assignments = guardianAssignmentRepository.findByCompanyIdAndChildId(
                            query.companyId.value,
                            child.id.value
                        )

                        // Wybierz głównego opiekuna lub pierwszego dostępnego
                        val primaryAssignment = assignments.find { it.isPrimary } ?: assignments.firstOrNull()

                        if (primaryAssignment != null) {
                            val guardian = guardianRepository.findByIdAndCompanyId(
                                primaryAssignment.guardianId,
                                query.companyId.value
                            )

                            if (guardian != null) {
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
                                    guardianFirstName = guardian.firstName,
                                    guardianLastName = guardian.lastName,
                                    guardianPhone = guardian.phone
                                )
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }.awaitAll().flatten()

            // Sortowanie: pickupTime ASC, lastName ASC, firstName ASC
            results.sortedWith(
                compareBy(
                    { it.pickupTime },
                    { it.lastName },
                    { it.firstName }
                )
            )
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