// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/getbyid/GetDriverAbsenceHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.getbyid

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceType
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class GetDriverAbsenceQuery(
    val companyId: CompanyId,
    val id: DriverAbsenceId
)

data class DriverAbsenceDetail(
    val id: DriverAbsenceId,
    val driverId: DriverId,
    val absenceType: DriverAbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
    val status: DriverAbsenceStatus,
    val createdBy: UserId,
    val createdByRole: UserRole,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val cancellationReason: String?
)

class DriverAbsenceNotFoundException(id: DriverAbsenceId) :
    NotFoundException("Driver absence ${id.value} not found")

@Component
class GetDriverAbsenceHandler(
    private val absenceRepository: DriverAbsenceRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: GetDriverAbsenceQuery): DriverAbsenceDetail {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val absence = absenceRepository.findById(query.companyId, query.id)
            ?: throw DriverAbsenceNotFoundException(query.id)

        return DriverAbsenceDetail(
            id = absence.id,
            driverId = absence.driverId,
            absenceType = absence.absenceType,
            startDate = absence.startDate,
            endDate = absence.endDate,
            reason = absence.reason,
            status = absence.getCurrentStatus(),
            createdBy = absence.createdBy,
            createdByRole = absence.createdByRole,
            createdAt = absence.createdAt,
            cancelledAt = absence.cancelledAt,
            cancellationReason = absence.cancellationReason
        )
    }
}