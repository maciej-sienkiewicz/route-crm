package pl.sienkiewiczmaciej.routecrm.shared.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
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

open class NotFoundException(message: String) : RuntimeException(message)