// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/cancel/CancelDriverAbsenceHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.cancel

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.driver.absence.getbyid.DriverAbsenceNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant

data class CancelDriverAbsenceCommand(
    val companyId: CompanyId,
    val id: DriverAbsenceId,
    val reason: String
)

data class CancelDriverAbsenceResult(
    val id: DriverAbsenceId,
    val cancelledAt: Instant
)

@Component
class CancelDriverAbsenceHandler(
    private val absenceRepository: DriverAbsenceRepository,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CancelDriverAbsenceCommand): CancelDriverAbsenceResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val absence = absenceRepository.findById(command.companyId, command.id)
            ?: throw DriverAbsenceNotFoundException(command.id)

        val cancelled = absence.cancel(command.reason)
        absenceRepository.save(cancelled)

        return CancelDriverAbsenceResult(
            id = cancelled.id,
            cancelledAt = cancelled.cancelledAt!!
        )
    }
}