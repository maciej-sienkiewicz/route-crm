// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/create/CreateDriverAbsenceHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.create

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsence
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceType
import pl.sienkiewiczmaciej.routecrm.driver.absence.infrastructure.services.DriverAbsenceApplicationService
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class CreateDriverAbsenceCommand(
    val companyId: CompanyId,
    val driverId: DriverId,
    val absenceType: DriverAbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?
)

data class CreateDriverAbsenceResult(
    val id: DriverAbsenceId,
    val driverId: DriverId,
    val absenceType: DriverAbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
    val status: DriverAbsenceStatus,
    val conflictingRoutesCount: Int,
    val createdBy: UserId,
)

@Component
class CreateDriverAbsenceHandler(
    private val validatorComposite: CreateDriverAbsenceValidatorComposite,
    private val absenceApplicationService: DriverAbsenceApplicationService,
    private val authService: AuthorizationService
) {
    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateDriverAbsenceCommand): CreateDriverAbsenceResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val context = validatorComposite.validate(command)

        val absence = DriverAbsence.create(
            companyId = command.companyId,
            driverId = command.driverId,
            absenceType = command.absenceType,
            startDate = command.startDate,
            endDate = command.endDate,
            reason = command.reason,
            createdBy = principal.userId,
            createdByRole = principal.role
        )

        val saved = context.absenceRepository.save(absence)

        val conflictingRoutes = absenceApplicationService.findConflictingRoutes(saved)

        return CreateDriverAbsenceResult(
            id = saved.id,
            driverId = saved.driverId,
            absenceType = saved.absenceType,
            startDate = saved.startDate,
            endDate = saved.endDate,
            reason = saved.reason,
            status = saved.status,
            conflictingRoutesCount = conflictingRoutes.size,
            createdBy = principal.userId
        )
    }
}