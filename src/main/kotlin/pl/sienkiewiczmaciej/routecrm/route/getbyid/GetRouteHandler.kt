package pl.sienkiewiczmaciej.routecrm.route.getbyid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class GetRouteQuery(
    val companyId: CompanyId,
    val id: RouteId
)

data class RouteChildDetail(
    val childId: ChildId,
    val scheduleId: ScheduleId,
    val firstName: String,
    val lastName: String,
    val pickupOrder: Int,
    val pickupAddress: ScheduleAddress,
    val dropoffAddress: ScheduleAddress,
    val estimatedPickupTime: LocalTime,
    val estimatedDropoffTime: LocalTime,
    val actualPickupTime: Instant?,
    val actualDropoffTime: Instant?,
    val status: ChildInRouteStatus,
    val guardianFirstName: String,
    val guardianLastName: String,
    val guardianPhone: String
)

data class RouteDetail(
    val id: RouteId,
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: DriverId,
    val driverFirstName: String,
    val driverLastName: String,
    val vehicleId: VehicleId,
    val vehicleRegistrationNumber: String,
    val vehicleModel: String,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val children: List<RouteChildDetail>,
    val notes: List<RouteNote>
)

class RouteNotFoundException(id: RouteId) : NotFoundException("Route ${id.value} not found")

@Component
class GetRouteHandler(
    private val routeRepository: RouteRepository,
    private val routeChildRepository: RouteChildRepository,
    private val routeNoteRepository: RouteNoteRepository,
    private val driverRepository: DriverJpaRepository,
    private val vehicleRepository: VehicleJpaRepository,
    private val childRepository: ChildJpaRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val guardianRepository: GuardianJpaRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetRouteQuery): RouteDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val route = routeRepository.findById(query.companyId, query.id)
            ?: throw RouteNotFoundException(query.id)

        // Dla kierowcy - sprawdź czy ma dostęp do tej trasy
        if (principal.role == UserRole.DRIVER && principal.driverId != null) {
            require(route.driverId == DriverId.from(principal.driverId)) {
                "Driver can only access their own routes"
            }
        }

        return withContext(Dispatchers.IO) {
            val driverDeferred = async {
                driverRepository.findByIdAndCompanyId(route.driverId.value, query.companyId.value)
            }

            val vehicleDeferred = async {
                vehicleRepository.findByIdAndCompanyId(route.vehicleId.value, query.companyId.value)
            }

            val routeChildrenDeferred = async {
                routeChildRepository.findByRoute(query.companyId, query.id)
            }

            val notesDeferred = async {
                routeNoteRepository.findByRoute(query.companyId, query.id)
            }

            val driver = driverDeferred.await()
                ?: throw NotFoundException("Driver ${route.driverId.value} not found")

            val vehicle = vehicleDeferred.await()
                ?: throw NotFoundException("Vehicle ${route.vehicleId.value} not found")

            val routeChildren = routeChildrenDeferred.await()
            val notes = notesDeferred.await()

            // Pobierz szczegóły dzieci i ich opiekunów
            val childrenDetails = routeChildren.map { routeChild ->
                async {
                    val child = childRepository.findByIdAndCompanyId(
                        routeChild.childId.value,
                        query.companyId.value
                    ) ?: throw NotFoundException("Child ${routeChild.childId.value} not found")

                    // Pobierz głównego opiekuna dziecka
                    val assignments = guardianAssignmentRepository.findByCompanyIdAndChildId(
                        query.companyId.value,
                        routeChild.childId.value
                    )
                    
                    val primaryAssignment = assignments.find { it.isPrimary } ?: assignments.firstOrNull()
                    
                    val (guardianFirstName, guardianLastName, guardianPhone) = if (primaryAssignment != null) {
                        val guardian = guardianRepository.findByIdAndCompanyId(
                            primaryAssignment.guardianId,
                            query.companyId.value
                        )
                        if (guardian != null) {
                            Triple(guardian.firstName, guardian.lastName, guardian.phone)
                        } else {
                            Triple("", "", "")
                        }
                    } else {
                        Triple("", "", "")
                    }

                    RouteChildDetail(
                        childId = routeChild.childId,
                        scheduleId = routeChild.scheduleId,
                        firstName = child.firstName,
                        lastName = child.lastName,
                        pickupOrder = routeChild.pickupOrder,
                        pickupAddress = routeChild.pickupAddress,
                        dropoffAddress = routeChild.dropoffAddress,
                        estimatedPickupTime = routeChild.estimatedPickupTime,
                        estimatedDropoffTime = routeChild.estimatedDropoffTime,
                        actualPickupTime = routeChild.actualPickupTime,
                        actualDropoffTime = routeChild.actualDropoffTime,
                        status = routeChild.status,
                        guardianFirstName = guardianFirstName,
                        guardianLastName = guardianLastName,
                        guardianPhone = guardianPhone
                    )
                }
            }.awaitAll()

            // Dla opiekuna - sprawdź czy ma dostęp do dzieci na tej trasie
            if (principal.role == UserRole.GUARDIAN && principal.guardianId != null) {
                val guardianAssignments = guardianAssignmentRepository.findByCompanyIdAndGuardianId(
                    query.companyId.value,
                    principal.guardianId
                )
                val guardianChildIds = guardianAssignments.map { it.childId }.toSet()
                
                val routeHasGuardianChild = childrenDetails.any { 
                    it.childId.value in guardianChildIds 
                }
                
                require(routeHasGuardianChild) {
                    "Guardian can only access routes with their children"
                }
            }

            RouteDetail(
                id = route.id,
                companyId = route.companyId,
                routeName = route.routeName,
                date = route.date,
                status = route.status,
                driverId = route.driverId,
                driverFirstName = driver.firstName,
                driverLastName = driver.lastName,
                vehicleId = route.vehicleId,
                vehicleRegistrationNumber = vehicle.registrationNumber,
                vehicleModel = vehicle.model,
                estimatedStartTime = route.estimatedStartTime,
                estimatedEndTime = route.estimatedEndTime,
                actualStartTime = route.actualStartTime,
                actualEndTime = route.actualEndTime,
                children = childrenDetails.sortedBy { it.pickupOrder },
                notes = notes
            )
        }
    }
}
