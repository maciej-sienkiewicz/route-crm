package pl.sienkiewiczmaciej.routecrm.driver.api.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.driver.api.dto.DriverRouteChangeDTO
import pl.sienkiewiczmaciej.routecrm.driver.api.dto.DriverRouteChangesResponse
import pl.sienkiewiczmaciej.routecrm.driver.api.dto.toDTO
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

@Service
class RouteChangeDetectionService(
    private val stopRepository: RouteStopRepository,
    private val childRepository: ChildRepository
) {

    suspend fun detectChanges(
        companyId: CompanyId,
        routeId: RouteId,
        since: Instant
    ): DriverRouteChangesResponse = coroutineScope {
        val currentStops = stopRepository.findByRoute(companyId, routeId, includeCancelled = true)

        val changes = mutableListOf<DriverRouteChangeDTO>()

        val cancelledStops = currentStops
            .filter { it.isCancelled && (it.cancelledAt?.isAfter(since) == true) }
            .map { stop ->
                DriverRouteChangeDTO.StopCancelled(
                    stopId = stop.id.value,
                    timestamp = stop.cancelledAt!!,
                    reason = stop.cancellationReason
                )
            }

        changes.addAll(cancelledStops)

        val newStops = currentStops.filter { stop ->
            val stopEntity = stopRepository.findById(companyId, stop.id)
            stopEntity != null && !stop.isCancelled
        }

        val addedStops = newStops.map { stop ->
            async {
                val child = childRepository.findById(companyId, stop.childId)
                    ?: return@async null

                DriverRouteChangeDTO.StopAdded(
                    stop = stop.toDTO(child),
                    insertAfter = stop.stopOrder - 1,
                    timestamp = Instant.now()
                )
            }
        }.awaitAll().filterNotNull()

        changes.addAll(addedStops)

        val lastModified = changes.maxOfOrNull { it.timestamp }

        DriverRouteChangesResponse(
            hasChanges = changes.isNotEmpty(),
            lastModified = lastModified,
            changes = changes.sortedBy { it.timestamp }
        )
    }
}