package pl.sienkiewiczmaciej.routecrm.driver.api.dto

import java.time.Instant

data class DriverRouteChangesResponse(
    val hasChanges: Boolean,
    val lastModified: Instant?,
    val changes: List<DriverRouteChangeDTO>
)

sealed class DriverRouteChangeDTO {
    abstract val timestamp: Instant

    data class StopCancelled(
        val stopId: String,
        override val timestamp: Instant,
        val reason: String?
    ) : DriverRouteChangeDTO()

    data class StopAdded(
        val stop: DriverRouteStopDTO,
        val insertAfter: Int,
        override val timestamp: Instant
    ) : DriverRouteChangeDTO()
}