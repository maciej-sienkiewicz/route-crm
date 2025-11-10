// route/getbyid/GetRouteHandler.kt (REFACTORED)
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

data class RouteStopDetail(
    val id: RouteStopId,
    val stopOrder: Int,
    val stopType: StopType,
    val childId: ChildId,
    val childFirstName: String,
    val childLastName: String,
    val scheduleId: ScheduleId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress,
    val isCancelled: Boolean,
    val cancelledAt: Instant?,
    val cancellationReason: String?,
    val actualTime: Instant?,
    val executionStatus: ExecutionStatus?,
    val executionNotes: String?,
    val executedByName: String?,
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
    val stops: List<RouteStopDetail>,
    val notes: List<RouteNote>
)

class RouteNotFoundException(id: RouteId) : NotFoundException("Route ${id.value} not found")

/**
 * Refactored GetRouteHandler - simplified to ~30 lines.
 * Query logic remains complex due to data enrichment needs.
 */
@Component
class GetRouteHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val noteRepository: RouteNoteRepository,
    private val driverRepository: DriverJpaRepository,
    private val vehicleRepository: VehicleJpaRepository,
    private val childRepository: ChildJpaRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val guardianRepository: GuardianJpaRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetRouteQuery): RouteDetail {
        // 1. Authorization
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        // 2. Load route
        val route = routeRepository.findById(query.companyId, query.id)
            ?: throw RouteNotFoundException(query.id)

        // 3. Role-specific authorization
        checkRoleSpecificAccess(principal, route)

        // 4. Load and enrich data (parallel queries)
        return buildRouteDetail(query, route, principal)
    }

    private fun checkRoleSpecificAccess(principal: UserPrincipal, route: Route) {
        when (principal.role) {
            UserRole.DRIVER -> {
                if (principal.driverId != null) {
                    require(route.driverId == DriverId.from(principal.driverId)) {
                        "Driver can only access their own routes"
                    }
                }
            }
            UserRole.GUARDIAN -> {
                // Guardian access check done after loading stops
            }
            else -> { /* ADMIN/OPERATOR have full access */ }
        }
    }

    private suspend fun buildRouteDetail(
        query: GetRouteQuery,
        route: Route,
        principal: UserPrincipal
    ): RouteDetail = withContext(Dispatchers.IO) {
        // Parallel data loading
        val driverDeferred = async {
            driverRepository.findByIdAndCompanyId(route.driverId.value, query.companyId.value)
                ?: throw NotFoundException("Driver ${route.driverId.value} not found")
        }

        val vehicleDeferred = async {
            vehicleRepository.findByIdAndCompanyId(route.vehicleId.value, query.companyId.value)
                ?: throw NotFoundException("Vehicle ${route.vehicleId.value} not found")
        }

        val stopsDeferred = async {
            stopRepository.findByRoute(query.companyId, query.id, includeCancelled = true)
        }

        val notesDeferred = async {
            noteRepository.findByRoute(query.companyId, query.id)
        }

        val driver = driverDeferred.await()
        val vehicle = vehicleDeferred.await()
        val stops = stopsDeferred.await()
        val notes = notesDeferred.await()

        // Enrich stops with child and guardian data
        val stopsDetails = stops.map { stop ->
            async { enrichStopWithDetails(stop, query.companyId) }
        }.awaitAll()

        // Guardian access check
        if (principal.role == UserRole.GUARDIAN && principal.guardianId != null) {
            checkGuardianAccess(principal.guardianId, stopsDetails, query.companyId)
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
            stops = stopsDetails.sortedBy { it.stopOrder },
            notes = notes
        )
    }

    private suspend fun enrichStopWithDetails(
        stop: RouteStop,
        companyId: CompanyId
    ): RouteStopDetail {
        val child = childRepository.findByIdAndCompanyId(stop.childId.value, companyId.value)
            ?: throw NotFoundException("Child ${stop.childId.value} not found")

        val assignments = guardianAssignmentRepository.findByCompanyIdAndChildId(
            companyId.value,
            stop.childId.value
        )

        val primaryAssignment = assignments.find { it.isPrimary } ?: assignments.firstOrNull()
        val (guardianFirstName, guardianLastName, guardianPhone) = if (primaryAssignment != null) {
            val guardian = guardianRepository.findByIdAndCompanyId(
                primaryAssignment.guardianId,
                companyId.value
            )
            if (guardian != null) {
                Triple(guardian.firstName, guardian.lastName, guardian.phone)
            } else {
                Triple("", "", "")
            }
        } else {
            Triple("", "", "")
        }

        return RouteStopDetail(
            id = stop.id,
            stopOrder = stop.stopOrder,
            stopType = stop.stopType,
            childId = stop.childId,
            childFirstName = child.firstName,
            childLastName = child.lastName,
            scheduleId = stop.scheduleId,
            estimatedTime = stop.estimatedTime,
            address = stop.address,
            isCancelled = stop.isCancelled,
            cancelledAt = stop.cancelledAt,
            cancellationReason = stop.cancellationReason,
            actualTime = stop.actualTime,
            executionStatus = stop.executionStatus,
            executionNotes = stop.executionNotes,
            executedByName = stop.executedByName,
            guardianFirstName = guardianFirstName,
            guardianLastName = guardianLastName,
            guardianPhone = guardianPhone
        )
    }

    private suspend fun checkGuardianAccess(
        guardianId: String,
        stopsDetails: List<RouteStopDetail>,
        companyId: CompanyId
    ) {
        val guardianAssignments = guardianAssignmentRepository.findByCompanyIdAndGuardianId(
            companyId.value,
            guardianId
        )
        val guardianChildIds = guardianAssignments.map { it.childId }.toSet()

        val routeHasGuardianChild = stopsDetails.any { it.childId.value in guardianChildIds }
        require(routeHasGuardianChild) {
            "Guardian can only access routes with their children"
        }
    }
}