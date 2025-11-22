package pl.sienkiewiczmaciej.routecrm.shared.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import pl.sienkiewiczmaciej.routecrm.routeseries.create.ScheduleConflictException
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.ForbiddenException
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.UnauthorizedException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException) = ResponseEntity
        .status(NOT_FOUND)
        .body(ErrorResponse(ex.message))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException) = ResponseEntity
        .status(UNAUTHORIZED)
        .body(ErrorResponse(ex.message))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException) = ResponseEntity
        .status(FORBIDDEN)
        .body(ErrorResponse(ex.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException) = ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(ex.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(ValidationErrorResponse("Validation failed", errors))
    }

    @ExceptionHandler(ScheduleConflictException::class)
    fun handleScheduleConflict(ex: ScheduleConflictException): ResponseEntity<ScheduleConflictErrorResponse> {
        val conflictData = ex.getConflictData()
        return ResponseEntity
            .status(CONFLICT)
            .body(
                ScheduleConflictErrorResponse(
                    message = "Schedule conflicts detected",
                    conflicts = ScheduleConflicts(
                        singleRoutes = conflictData.singleRoutes,
                        series = conflictData.series
                    )
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("Internal server error"))
    }
}

data class ErrorResponse(
    val message: String?,
    val timestamp: Instant = Instant.now()
)

data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, String> = emptyMap(),
    val timestamp: Instant = Instant.now()
)

/**
 * Structured conflicts data.
 * singleRoutes: Conflicts with individual routes - grouped by child name with dates
 * series: Conflicts with route series - grouped by child name with series name
 */
data class ScheduleConflicts(
    val singleRoutes: Map<String, List<String>>,
    val series: Map<String, String>
)

/**
 * Response for schedule conflict errors.
 *
 * Example JSON:
 * {
 *   "message": "Schedule conflicts detected",
 *   "conflicts": {
 *     "singleRoutes": {
 *       "Jan Kowalski": ["2024-01-15", "2024-01-22"],
 *       "Anna Nowak": ["2024-01-15"]
 *     },
 *     "series": {
 *       "Jan Kowalski": "Trasa Poniedziałkowa",
 *       "Piotr Wiśniewski": "Trasa Środowa"
 *     }
 *   },
 *   "timestamp": "2024-01-10T10:30:00Z"
 * }
 */
data class ScheduleConflictErrorResponse(
    val message: String,
    val conflicts: ScheduleConflicts,
    val timestamp: Instant = Instant.now()
)

open class NotFoundException(message: String) : RuntimeException(message)