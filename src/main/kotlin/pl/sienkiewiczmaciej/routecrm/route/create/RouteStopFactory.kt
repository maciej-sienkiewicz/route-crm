// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/create/RouteStopFactory.kt
package pl.sienkiewiczmaciej.routecrm.route.create

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.external.GeocodingService
import java.time.Instant

/**
 * Factory responsible for creating RouteStop domain objects.
 * Handles complex RouteStop creation logic including address enrichment with coordinates.
 */
@Component
class RouteStopFactory(
    private val geocodingService: GeocodingService
) {

    /**
     * Creates multiple RouteStop objects from stop data.
     * Enriches addresses with coordinates from schedules.
     *
     * @param routeId The ID of the route these stops belong to
     * @param companyId The company ID
     * @param stopsData List of stop data to create stops from
     * @param context Validated context with pre-loaded schedules
     * @return List of created RouteStop objects with enriched addresses
     */
    suspend fun createStops(
        routeId: RouteId,
        companyId: CompanyId,
        stopsData: List<RouteStopData>,
        context: CreateRouteValidationContext
    ): List<RouteStop> {
        return stopsData.map { stopData ->
            createStop(routeId, companyId, stopData, context)
        }
    }

    /**
     * Creates a single RouteStop with enriched address data.
     *
     * @param routeId The ID of the route this stop belongs to
     * @param companyId The company ID
     * @param stopData The stop data to create stop from
     * @param context Validated context with pre-loaded schedules
     * @return Created RouteStop object with enriched address
     */
    private suspend fun createStop(
        routeId: RouteId,
        companyId: CompanyId,
        stopData: RouteStopData,
        context: CreateRouteValidationContext
    ): RouteStop {
        // Get schedule from context (guaranteed to exist due to validation)
        val schedule = context.schedules[stopData.scheduleId]
            ?: throw IllegalStateException("Schedule ${stopData.scheduleId.value} not found in context")

        // Enrich address with coordinates
        val addressWithCoords = enrichAddressWithCoordinates(
            address = stopData.address,
            schedule = schedule,
            stopType = stopData.stopType
        )

        return RouteStop.create(
            companyId = companyId,
            routeId = routeId,
            stopOrder = stopData.stopOrder,
            stopType = stopData.stopType,
            childId = stopData.childId,
            scheduleId = stopData.scheduleId,
            estimatedTime = stopData.estimatedTime,
            address = addressWithCoords,
            createdAt = Instant.now()
        )
    }

    /**
     * Enriches an address with coordinates from the schedule.
     * Uses schedule's pickup or dropoff coordinates depending on stop type.
     * Falls back to geocoding if schedule doesn't have coordinates.
     *
     * @param address The address to enrich
     * @param schedule The schedule containing coordinate information
     * @param stopType The type of stop (PICKUP or DROPOFF)
     * @return Address enriched with coordinates
     */
    private suspend fun enrichAddressWithCoordinates(
        address: ScheduleAddress,
        schedule: Schedule,
        stopType: StopType
    ): ScheduleAddress {
        // Get coordinates from schedule based on stop type
        val scheduleAddress = when (stopType) {
            StopType.PICKUP -> schedule.pickupAddress
            StopType.DROPOFF -> schedule.dropoffAddress
        }

        // If address already has coordinates, use them
        if (address.latitude != null && address.longitude != null) {
            return address
        }

        // If schedule has coordinates, use them
        if (scheduleAddress.latitude != null && scheduleAddress.longitude != null) {
            return address.copy(
                latitude = scheduleAddress.latitude,
                longitude = scheduleAddress.longitude
            )
        }

        // Fall back to geocoding if no coordinates available
        val geocodingResult = geocodingService.geocodeAddress(address.address)

        return if (geocodingResult != null) {
            address.copy(
                latitude = geocodingResult.latitude,
                longitude = geocodingResult.longitude
            )
        } else {
            // Return address without coordinates if geocoding fails
            address
        }
    }
}