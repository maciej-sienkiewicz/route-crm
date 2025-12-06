package pl.sienkiewiczmaciej.routecrm.driver.api.exceptions

sealed class DriverApiException(message: String) : RuntimeException(message)

class RouteNotAssignedException(routeId: String) :
    DriverApiException("Route $routeId is not assigned to you")

class RouteAlreadyStartedException(routeId: String) :
    DriverApiException("Route $routeId is already started")

class DriverHasActiveRouteException(activeRouteId: String) :
    DriverApiException("You already have an active route: $activeRouteId")

class StopAlreadyExecutedException(stopId: String) :
    DriverApiException("Stop $stopId is already executed")

class InvalidStopActionException(action: String) :
    DriverApiException("Invalid stop action: $action")

class RouteNotInProgressException(routeId: String) :
    DriverApiException("Route $routeId is not in progress")

class StopNotFoundException(stopId: String) :
    DriverApiException("Stop $stopId not found")

class RouteNotFoundException(routeId: String) :
    DriverApiException("Route $routeId not found")

class InvalidTimestampException(hours: Long) :
    DriverApiException("Operation timestamp too old: $hours hours ago")

class CannotExecuteCancelledStopException(stopId: String) :
    DriverApiException("Cannot execute cancelled stop: $stopId")

class InvalidRouteDateException :
    DriverApiException("Cannot start route from different day")

class DriverIdMissingException :
    DriverApiException("Driver ID not found in authentication")

class NoUpcomingRouteException :
    DriverApiException("No upcoming route found for today")