// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/list/ListDriverAbsencesHandler.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.list

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceId
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceStatus
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceType
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.Instant
import java.time.LocalDate

data class ListDriverAbsencesQuery(
    val companyId: CompanyId,
    val driverId: DriverId,
    val from: LocalDate?,
    val to: LocalDate?,
    val statuses: Set<DriverAbsenceStatus>?
)

data class DriverAbsenceListItem(
    val id: DriverAbsenceId,
    val driverId: DriverId,
    val absenceType: DriverAbsenceType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
    val status: DriverAbsenceStatus,
    val createdByRole: UserRole,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val cancellationReason: String?
)

@Component
class ListDriverAbsencesHandler(
    private val absenceRepository: DriverAbsenceRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListDriverAbsencesQuery): List<DriverAbsenceListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val absences = absenceRepository.findByDriver(
            companyId = query.companyId,
            driverId = query.driverId,
            from = query.from,
            to = query.to,
            statuses = query.statuses
        )

        return absences.map { absence ->
            DriverAbsenceListItem(
                id = absence.id,
                driverId = absence.driverId,
                absenceType = absence.absenceType,
                startDate = absence.startDate,
                endDate = absence.endDate,
                reason = absence.reason,
                status = absence.getCurrentStatus(),
                createdByRole = absence.createdByRole,
                createdAt = absence.createdAt,
                cancelledAt = absence.cancelledAt,
                cancellationReason = absence.cancellationReason
            )
        }
    }
}