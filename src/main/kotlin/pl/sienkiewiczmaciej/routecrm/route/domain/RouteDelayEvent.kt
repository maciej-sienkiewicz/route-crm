package pl.sienkiewiczmaciej.routecrm.route.domain

import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.util.*

data class RouteDelayEvent(
    val id: RouteDelayEventId,
    val companyId: CompanyId,
    val routeId: RouteId,
    val stopId: RouteStopId,
    val delayMinutes: Int,
    val detectionType: DelayDetectionType,
    val detectedAt: Instant
) {
    companion object {
        fun retrospective(
            companyId: CompanyId,
            routeId: RouteId,
            stopId: RouteStopId,
            delayMinutes: Int,
            detectedAt: Instant
        ) = RouteDelayEvent(
            id = RouteDelayEventId(UUID.randomUUID().toString()),
            companyId = companyId,
            routeId = routeId,
            stopId = stopId,
            delayMinutes = delayMinutes,
            detectionType = DelayDetectionType.RETROSPECTIVE,
            detectedAt = detectedAt
        )

        fun predictive(
            companyId: CompanyId,
            routeId: RouteId,
            stopId: RouteStopId,
            delayMinutes: Int,
            detectedAt: Instant
        ) = RouteDelayEvent(
            id = RouteDelayEventId(UUID.randomUUID().toString()),
            companyId = companyId,
            routeId = routeId,
            stopId = stopId,
            delayMinutes = delayMinutes,
            detectionType = DelayDetectionType.PREDICTIVE,
            detectedAt = detectedAt
        )
    }
}

@JvmInline
value class RouteDelayEventId(val value: String)

enum class DelayDetectionType {
    RETROSPECTIVE,
    PREDICTIVE
}