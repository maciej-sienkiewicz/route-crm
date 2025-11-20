package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.misc.NearestPointOnLineProps
import org.maplibre.spatialk.turf.misc.nearestPointTo
import org.maplibre.spatialk.units.International.Meters
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule

data class RouteStops(
    val routeId: RouteId,
    val stops: List<RouteStop>
)

@Component
class RouteSuggestionService() {
    fun findSuggestions(routes: List<RouteStops>, newSchedule: Schedule): List<RouteId> {

        val routeStops = routes.associate { it.routeId to it.stops.sortedBy { it.stopOrder }.map { it.address } }

        val newPickupPoint = Point(newSchedule.pickupAddress.longitude!!, newSchedule.pickupAddress.latitude!!).coordinates
        val newTargetPoint = Point(newSchedule.dropoffAddress.longitude!!, newSchedule.dropoffAddress.latitude!!).coordinates

        val suggestions = mutableListOf<RouteId>()

        routesLoop@ for ((routeId, stops) in routeStops) {
            if (stops.size < 2) {
                continue@routesLoop
            }

            val routeCoords: List<Position> = stops.map {
                Position(it.longitude!!, it.latitude!!)
            }

            val fullRouteLine = buildPolyline(routeCoords)
            val nearestPointToPickup = fullRouteLine.nearestPointTo(newPickupPoint)

            if (distanceToAddressFromRouteLine(nearestPointToPickup) > MAX_COVERAGE_DISTANCE_IN_METERS) {
                continue@routesLoop
            }

            val nearestPointIndex = nearestPointToPickup.properties.index
            val remainingRouteCoords = routeCoords.drop(nearestPointIndex)
            if (remainingRouteCoords.size <= 2) {
                suggestions.add(routeId)
                continue@routesLoop
            }

            val remainingRouteLine = buildPolyline(remainingRouteCoords)
            val nearestPointToDropoff = remainingRouteLine.nearestPointTo(newTargetPoint)

            if (distanceToAddressFromRouteLine(nearestPointToDropoff) > MAX_COVERAGE_DISTANCE_IN_METERS) {
                continue@routesLoop
            }

            suggestions.add(routeId)
        }

        return suggestions
    }

    private fun distanceToAddressFromRouteLine(point: Feature<Point, NearestPointOnLineProps>): Double =
        point.properties.distance.toDouble(Meters)

    private fun buildPolyline(routeCoords: List<Position>): LineString = LineString(routeCoords)

    companion object {
        private const val MAX_COVERAGE_DISTANCE_IN_METERS = 1000L
    }
}