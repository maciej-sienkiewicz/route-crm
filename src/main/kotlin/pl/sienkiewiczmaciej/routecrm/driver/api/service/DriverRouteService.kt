package pl.sienkiewiczmaciej.routecrm.driver.api.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.driver.api.dto.*
import pl.sienkiewiczmaciej.routecrm.driver.api.exceptions.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class DriverRouteService(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val vehicleRepository: VehicleRepository,
    private val childRepository: ChildRepository,
    private val delayEventRepository: RouteDelayEventRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getUpcomingRoute(principal: UserPrincipal): DriverRouteDTO = coroutineScope {
        val driverId = principal.extractDriverId()

        val routes = routeRepository.findByDriver(
            companyId = principal.companyId,
            driverId = driverId,
            date = LocalDate.now(),
            pageable = PageRequest.of(0, 10)
        ).content

        val upcomingRoute = routes
            .filter { it.status in listOf(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS) }
            .minByOrNull { it.estimatedStartTime }
            ?: throw NoUpcomingRouteException()

        val vehicle = async {
            vehicleRepository.findById(principal.companyId, upcomingRoute.vehicleId)
                ?: error("Vehicle not found: ${upcomingRoute.vehicleId}")
        }

        val stops = async {
            stopRepository.findByRoute(
                principal.companyId,
                upcomingRoute.id,
                includeCancelled = false
            )
        }

        upcomingRoute.toDriverDTO(
            vehicle = vehicle.await(),
            stopsTotal = stops.await().size,
            stopsCompleted = stops.await().count { it.executionStatus == ExecutionStatus.COMPLETED }
        )
    }

    suspend fun getRouteDetail(
        principal: UserPrincipal,
        routeId: RouteId
    ): RouteDetailResponse = coroutineScope {
        val route = routeRepository.findById(principal.companyId, routeId)
            ?: throw RouteNotFoundException(routeId.value)

        route.verifyAssignedTo(principal.extractDriverId())

        val vehicleDeferred = async {
            vehicleRepository.findById(principal.companyId, route.vehicleId)
                ?: error("Vehicle not found: ${route.vehicleId}")
        }

        val stopsDeferred = async {
            stopRepository.findByRoute(principal.companyId, route.id, includeCancelled = false)
        }

        val vehicle = vehicleDeferred.await()
        val stops = stopsDeferred.await()

        val stopDTOs = stops.map { stop ->
            async {
                val child = childRepository.findById(principal.companyId, stop.childId)
                    ?: error("Child not found: ${stop.childId}")
                stop.toDTO(child)
            }
        }.awaitAll()

        RouteDetailResponse(
            route = route.toDriverDTO(
                vehicle = vehicle,
                stopsTotal = stops.size,
                stopsCompleted = stops.count { it.executionStatus == ExecutionStatus.COMPLETED }
            ),
            stops = stopDTOs
        )
    }

    @Transactional
    suspend fun startRoute(principal: UserPrincipal, routeId: RouteId): DriverRouteDTO {
        val route = routeRepository.findById(principal.companyId, routeId)
            ?: throw RouteNotFoundException(routeId.value)

        val driverId = principal.extractDriverId()

        route.verifyAssignedTo(driverId)
        route.verifyCanStart()
        verifyNoActiveRoute(principal, driverId)

        val startedRoute = route.start(Instant.now())
        val saved = routeRepository.save(startedRoute)

        val vehicle = vehicleRepository.findById(principal.companyId, route.vehicleId)
            ?: error("Vehicle not found")

        val stops = stopRepository.findByRoute(principal.companyId, route.id, includeCancelled = false)

        return saved.toDriverDTO(
            vehicle = vehicle,
            stopsTotal = stops.size,
            stopsCompleted = stops.count { it.executionStatus == ExecutionStatus.COMPLETED }
        )
    }

    @Transactional
    suspend fun executeStop(
        principal: UserPrincipal,
        stopId: RouteStopId,
        request: ExecuteStopRequest
    ): ExecuteStopResponse {
        val stop = stopRepository.findById(principal.companyId, stopId)
            ?: throw StopNotFoundException(stopId.value)

        val route = routeRepository.findById(principal.companyId, stop.routeId)
            ?: throw RouteNotFoundException(stop.routeId.value)

        route.verifyAssignedTo(principal.extractDriverId())
        route.verifyInProgress()
        stop.verifyCanExecute()

        val actualTime = request.timestamp.validateAndDefault()
        val action = request.action.parseStopAction()

        val updatedStop = when (action) {
            StopAction.COMPLETE -> stop.executeAsCompleted(
                actualTime,
                principal,
                request.notes
            )
            StopAction.SKIP -> stop.executeAsSkipped(
                actualTime,
                principal,
                request.reason.parseSkipReason(),
                request.notes
            )
        }

        val savedStop = stopRepository.save(updatedStop)

        detectAndPersistDelayIfNeeded(route, savedStop)

        val allStops = stopRepository.findByRoute(principal.companyId, route.id, includeCancelled = false)
        val allExecuted = allStops.all { it.executionStatus != null || it.isCancelled }

        val updatedRoute = if (allExecuted) {
            val lastStopTime = allStops.mapNotNull { it.actualTime }.maxOrNull() ?: Instant.now()
            routeRepository.save(route.complete(lastStopTime))
        } else {
            route
        }

        val child = childRepository.findById(principal.companyId, stop.childId)
            ?: error("Child not found")

        return ExecuteStopResponse(
            stop = savedStop.toDTO(child),
            route = RouteStatusDTO(
                id = updatedRoute.id.value,
                status = updatedRoute.status,
                stopsCompleted = allStops.count { it.executionStatus == ExecutionStatus.COMPLETED },
                stopsTotal = allStops.size
            )
        )
    }

    private suspend fun detectAndPersistDelayIfNeeded(route: Route, stop: RouteStop) {
        if (stop.actualTime == null) return

        val estimatedDateTime = stop.estimatedTime
            .atDate(route.date)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val delay = Duration.between(estimatedDateTime, stop.actualTime)

        if (delay.toMinutes() >= DELAY_THRESHOLD_MINUTES) {
            delayEventRepository.save(
                RouteDelayEvent.retrospective(
                    companyId = route.companyId,
                    routeId = route.id,
                    stopId = stop.id,
                    delayMinutes = delay.toMinutes().toInt(),
                    detectedAt = Instant.now()
                )
            )

            logger.info(
                "Retrospective delay detected in driver app: Route ${route.id.value}, " +
                        "Stop ${stop.id.value}, Delay: ${delay.toMinutes()} minutes"
            )
        }
    }

    private suspend fun verifyNoActiveRoute(principal: UserPrincipal, driverId: DriverId) {
        val activeRoutes = routeRepository.findByDriver(
            companyId = principal.companyId,
            driverId = driverId,
            date = null,
            pageable = PageRequest.of(0, 10)
        ).content.filter { it.status == RouteStatus.IN_PROGRESS }

        if (activeRoutes.isNotEmpty()) {
            throw DriverHasActiveRouteException(activeRoutes.first().id.value)
        }
    }

    private fun UserPrincipal.extractDriverId() =
        DriverId(driverId ?: throw DriverIdMissingException())

    private fun Route.verifyAssignedTo(driverId: DriverId) {
        if (this.driverId != driverId) {
            throw RouteNotAssignedException(id.value)
        }
    }

    private fun Route.verifyCanStart() {
        if (status != RouteStatus.PLANNED) {
            throw RouteAlreadyStartedException(id.value)
        }
        if (date != LocalDate.now()) {
            throw InvalidRouteDateException()
        }
    }

    private fun Route.verifyInProgress() {
        if (status != RouteStatus.IN_PROGRESS) {
            throw RouteNotInProgressException(id.value)
        }
    }

    private fun RouteStop.verifyCanExecute() {
        if (executionStatus != null) {
            throw StopAlreadyExecutedException(id.value)
        }
        if (isCancelled) {
            throw CannotExecuteCancelledStopException(id.value)
        }
    }

    private fun Instant?.validateAndDefault(): Instant {
        val serverTime = Instant.now()

        if (this == null) return serverTime

        val timeDiff = Duration.between(this, serverTime).abs()

        if (timeDiff > Duration.ofHours(24)) {
            throw InvalidTimestampException(timeDiff.toHours())
        }

        return this
    }

    private fun String.parseStopAction() = try {
        StopAction.valueOf(uppercase())
    } catch (e: IllegalArgumentException) {
        throw InvalidStopActionException(this)
    }

    private fun String?.parseSkipReason() = this?.let {
        try {
            SkipReason.valueOf(it.uppercase())
        } catch (e: IllegalArgumentException) {
            SkipReason.OTHER
        }
    } ?: SkipReason.OTHER

    private fun RouteStop.executeAsCompleted(
        actualTime: Instant,
        principal: UserPrincipal,
        notes: String?
    ) = execute(
        actualTime = actualTime,
        status = ExecutionStatus.COMPLETED,
        executedByUserId = principal.userId.value,
        executedByName = "${principal.firstName} ${principal.lastName}",
        notes = notes
    )

    private fun RouteStop.executeAsSkipped(
        actualTime: Instant,
        principal: UserPrincipal,
        reason: SkipReason,
        notes: String?
    ) = execute(
        actualTime = actualTime,
        status = ExecutionStatus.NO_SHOW,
        executedByUserId = principal.userId.value,
        executedByName = "${principal.firstName} ${principal.lastName}",
        notes = buildSkipNotes(reason, notes)
    )

    private fun buildSkipNotes(reason: SkipReason, additionalNotes: String?) =
        if (additionalNotes.isNullOrBlank()) {
            reason.name
        } else {
            "${reason.name}: $additionalNotes"
        }

    companion object {
        private const val DELAY_THRESHOLD_MINUTES = 3L
    }
}