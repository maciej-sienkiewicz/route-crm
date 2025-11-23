// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/suggestions/RouteSuggestionEnrichmentService.kt
package pl.sienkiewiczmaciej.routecrm.route.suggestions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Simplified route detail for suggestions - without notes, timestamps
 */
data class RouteSuggestionDetail(
    val id: RouteId,
    val companyId: CompanyId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driver: DriverSimple?,
    val vehicle: VehicleSimple,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val actualStartTime: Instant?,
    val actualEndTime: Instant?,
    val stops: List<RouteStopSimple>
)

data class DriverSimple(
    val id: DriverId,
    val firstName: String,
    val lastName: String
)

data class VehicleSimple(
    val id: VehicleId,
    val registrationNumber: String,
    val model: String
)

data class RouteStopSimple(
    val id: RouteStopId,
    val stopOrder: Int,
    val stopType: StopType,
    val childId: pl.sienkiewiczmaciej.routecrm.child.domain.ChildId,
    val childFirstName: String,
    val childLastName: String,
    val scheduleId: ScheduleId,
    val estimatedTime: LocalTime,
    val address: ScheduleAddress,
    val isCancelled: Boolean,
    val actualTime: Instant?,
    val executionStatus: ExecutionStatus?,
    val guardian: GuardianSimple
)

data class GuardianSimple(
    val firstName: String,
    val lastName: String,
    val phone: String
)

@Component
class RouteSuggestionEnrichmentService(
    private val stopRepository: RouteStopRepository,
    private val driverRepository: DriverJpaRepository,
    private val vehicleRepository: VehicleJpaRepository,
    private val childRepository: ChildJpaRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val guardianRepository: GuardianJpaRepository
) {
    suspend fun enrichRouteSuggestions(
        routeIds: List<RouteId>,
        availableRoutes: List<Route>,
        companyId: CompanyId
    ): List<RouteSuggestionDetail> = withContext(Dispatchers.IO) {
        val routeMap = availableRoutes.associateBy { it.id }

        routeIds.mapNotNull { routeId ->
            async {
                val route = routeMap[routeId] ?: return@async null
                enrichSingleRoute(route, companyId)
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun enrichSingleRoute(
        route: Route,
        companyId: CompanyId
    ): RouteSuggestionDetail = withContext(Dispatchers.IO) {
        val driver = route.driverId?.let { driverId ->
            driverRepository.findByIdAndCompanyId(
                driverId.value,
                companyId.value
            )
        }

        val vehicle = vehicleRepository.findByIdAndCompanyId(
            route.vehicleId.value,
            companyId.value
        )

        val stops = stopRepository.findByRoute(
            companyId = companyId,
            routeId = route.id,
            includeCancelled = false
        )

        val enrichedStops = stops.map { stop ->
            async { enrichStop(stop, companyId) }
        }.awaitAll()

        RouteSuggestionDetail(
            id = route.id,
            companyId = route.companyId,
            routeName = route.routeName,
            date = route.date,
            status = route.status,
            driver = if (route.driverId != null) { // ← ZMIENIONE: mapowanie nullable
                DriverSimple(
                    id = route.driverId,
                    firstName = driver?.firstName ?: "",
                    lastName = driver?.lastName ?: ""
                )
            } else null,
            vehicle = VehicleSimple(
                id = route.vehicleId,
                registrationNumber = vehicle?.registrationNumber ?: "",
                model = vehicle?.model ?: ""
            ),
            estimatedStartTime = route.estimatedStartTime,
            estimatedEndTime = route.estimatedEndTime,
            actualStartTime = route.actualStartTime,
            actualEndTime = route.actualEndTime,
            stops = enrichedStops.sortedBy { it.stopOrder }
        )
    }

    private suspend fun enrichStop(
        stop: RouteStop,
        companyId: CompanyId
    ): RouteStopSimple {
        val child = childRepository.findByIdAndCompanyId(
            stop.childId.value,
            companyId.value
        )

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

        return RouteStopSimple(
            id = stop.id,
            stopOrder = stop.stopOrder,
            stopType = stop.stopType,
            childId = stop.childId,
            childFirstName = child?.firstName ?: "",
            childLastName = child?.lastName ?: "",
            scheduleId = stop.scheduleId,
            estimatedTime = stop.estimatedTime,
            address = stop.address,
            isCancelled = stop.isCancelled,
            actualTime = stop.actualTime,
            executionStatus = stop.executionStatus,
            guardian = GuardianSimple(
                firstName = guardianFirstName,
                lastName = guardianLastName,
                phone = guardianPhone
            )
        )
    }
}