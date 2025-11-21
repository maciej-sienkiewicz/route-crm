// routeseries/addchild/AddChildValidators.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus

@Component
class AddChildSeriesStatusValidator {
    fun validate(context: AddChildValidationContext) {
        require(context.series.status == RouteSeriesStatus.ACTIVE) {
            "Cannot add child to series with status ${context.series.status}"
        }
    }
}

@Component
class AddChildScheduleOwnershipValidator {
    fun validate(context: AddChildValidationContext, command: AddChildToRouteSeriesCommand) {
        require(context.schedule.childId == command.childId) {
            "Schedule ${command.scheduleId.value} does not belong to child ${command.childId.value}"
        }
    }
}

@Component
class AddChildStatusValidator {
    fun validate(context: AddChildValidationContext) {
        require(context.child.status == ChildStatus.ACTIVE) {
            "Child must be ACTIVE, current status is ${context.child.status}"
        }
    }
}

@Component
class AddChildStopOrderValidator {
    fun validate(command: AddChildToRouteSeriesCommand) {
        require(command.pickupStopOrder > 0) {
            "Pickup stop order must be positive"
        }
        require(command.dropoffStopOrder > 0) {
            "Dropoff stop order must be positive"
        }
        require(command.pickupStopOrder < command.dropoffStopOrder) {
            "Pickup stop order must be before dropoff stop order"
        }
    }
}

@Component
class AddChildEffectiveDatesValidator {
    fun validate(command: AddChildToRouteSeriesCommand, context: AddChildValidationContext) {
        require(!command.effectiveFrom.isBefore(context.series.startDate)) {
            "Effective from date cannot be before series start date ${context.series.startDate}"
        }

        if (context.series.endDate != null) {
            require(!command.effectiveFrom.isAfter(context.series.endDate)) {
                "Effective from date cannot be after series end date ${context.series.endDate}"
            }
        }

        if (command.effectiveTo != null) {
            require(command.effectiveTo.isAfter(command.effectiveFrom)) {
                "Effective to date must be after effective from date"
            }
        }
    }
}