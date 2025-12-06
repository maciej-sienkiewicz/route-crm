package pl.sienkiewiczmaciej.routecrm.driver.api.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class DriverApiExceptionHandler {

    @ExceptionHandler(RouteNotAssignedException::class)
    fun handleRouteNotAssigned(ex: RouteNotAssignedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse("ROUTE_NOT_ASSIGNED", ex.message!!))

    @ExceptionHandler(RouteAlreadyStartedException::class)
    fun handleRouteAlreadyStarted(ex: RouteAlreadyStartedException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("ROUTE_ALREADY_STARTED", ex.message!!))

    @ExceptionHandler(DriverHasActiveRouteException::class)
    fun handleDriverHasActiveRoute(ex: DriverHasActiveRouteException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("DRIVER_HAS_ACTIVE_ROUTE", ex.message!!))

    @ExceptionHandler(StopAlreadyExecutedException::class)
    fun handleStopAlreadyExecuted(ex: StopAlreadyExecutedException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("STOP_ALREADY_EXECUTED", ex.message!!))

    @ExceptionHandler(InvalidStopActionException::class)
    fun handleInvalidStopAction(ex: InvalidStopActionException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_STOP_ACTION", ex.message!!))

    @ExceptionHandler(RouteNotInProgressException::class)
    fun handleRouteNotInProgress(ex: RouteNotInProgressException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("ROUTE_NOT_IN_PROGRESS", ex.message!!))

    @ExceptionHandler(StopNotFoundException::class)
    fun handleStopNotFound(ex: StopNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("STOP_NOT_FOUND", ex.message!!))

    @ExceptionHandler(RouteNotFoundException::class)
    fun handleRouteNotFound(ex: RouteNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("ROUTE_NOT_FOUND", ex.message!!))

    @ExceptionHandler(InvalidTimestampException::class)
    fun handleInvalidTimestamp(ex: InvalidTimestampException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_TIMESTAMP", ex.message!!))

    @ExceptionHandler(CannotExecuteCancelledStopException::class)
    fun handleCannotExecuteCancelledStop(ex: CannotExecuteCancelledStopException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse("CANNOT_EXECUTE_CANCELLED_STOP", ex.message!!))

    @ExceptionHandler(InvalidRouteDateException::class)
    fun handleInvalidRouteDate(ex: InvalidRouteDateException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_ROUTE_DATE", ex.message!!))

    @ExceptionHandler(DriverIdMissingException::class)
    fun handleDriverIdMissing(ex: DriverIdMissingException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse("DRIVER_ID_MISSING", ex.message!!))

    @ExceptionHandler(NoUpcomingRouteException::class)
    fun handleNoUpcomingRoute(ex: NoUpcomingRouteException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("NO_UPCOMING_ROUTE", ex.message!!))
}

data class ErrorResponse(
    val error: String,
    val message: String
)