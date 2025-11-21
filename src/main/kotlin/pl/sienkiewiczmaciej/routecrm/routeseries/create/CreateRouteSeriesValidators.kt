// routeseries/create/CreateRouteSeriesValidators.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.create

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleStatus

@Component
class SeriesNameValidator {
    fun validate(command: CreateRouteSeriesFromRouteCommand) {
        require(command.seriesName.isNotBlank()) {
            "Series name is required"
        }
        require(command.seriesName.length <= 255) {
            "Series name cannot exceed 255 characters"
        }
    }
}

@Component
class RecurrenceIntervalValidator {
    fun validate(command: CreateRouteSeriesFromRouteCommand) {
        require(command.recurrenceInterval in 1..4) {
            "Recurrence interval must be between 1 and 4 weeks"
        }
    }
}

@Component
class DateRangeValidator {
    fun validate(command: CreateRouteSeriesFromRouteCommand) {
        if (command.endDate != null) {
            require(command.endDate.isAfter(command.startDate)) {
                "End date must be after start date"
            }
        }
    }
}

@Component
class SourceRouteStatusValidator {
    fun validate(context: CreateRouteSeriesValidationContext) {
        require(context.sourceRoute.status == RouteStatus.PLANNED) {
            "Can only create series from PLANNED routes, route has status ${context.sourceRoute.status}"
        }
    }
}

@Component
class SeriesDriverStatusValidator {
    fun validate(context: CreateRouteSeriesValidationContext) {
        require(context.driver.status == DriverStatus.ACTIVE) {
            "Driver must be ACTIVE to create series"
        }
    }
}

@Component
class SeriesVehicleStatusValidator {
    fun validate(context: CreateRouteSeriesValidationContext) {
        require(context.vehicle.status == VehicleStatus.AVAILABLE) {
            "Vehicle must be AVAILABLE to create series"
        }
    }
}

@Component
class SeriesChildrenStatusValidator {
    fun validate(context: CreateRouteSeriesValidationContext) {
        context.children.values.forEach { child ->
            require(child.status == ChildStatus.ACTIVE) {
                "All children must be ACTIVE, child ${child.id.value} has status ${child.status}"
            }
        }
    }
}

@Component
class RouteStopPairsValidator {
    fun validate(context: CreateRouteSeriesValidationContext) {
        val stopsBySchedule = context.sourceRouteStops.groupBy { it.scheduleId }

        stopsBySchedule.forEach { (scheduleId, stops) ->
            require(stops.size == 2) {
                "Schedule ${scheduleId.value} must have exactly 2 stops (pickup and dropoff), found ${stops.size}"
            }

            val hasPickup = stops.any { it.stopType == pl.sienkiewiczmaciej.routecrm.route.domain.StopType.PICKUP }
            val hasDropoff = stops.any { it.stopType == pl.sienkiewiczmaciej.routecrm.route.domain.StopType.DROPOFF }

            require(hasPickup && hasDropoff) {
                "Schedule ${scheduleId.value} must have both pickup and dropoff stops"
            }
        }
    }
}