package pl.sienkiewiczmaciej.routecrm.driver.list

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalDate

data class ListDriversQuery(
    val companyId: CompanyId,
    val status: DriverStatus?,
    val search: String?,
    val pageable: Pageable
)

data class DriverListItem(
    val id: DriverId,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
    val status: DriverStatus,
    val licenseCategories: Set<String>,
    val licenseValidUntil: LocalDate,
    val medicalCertificateValidUntil: LocalDate,
    val todayRoutesCount: Int
)

@Component
class ListDriversHandler(
    private val driverRepository: DriverRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListDriversQuery): Page<DriverListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val drivers = driverRepository.findAll(
            companyId = query.companyId,
            status = query.status,
            search = query.search,
            pageable = query.pageable
        )

        return drivers.map { driver ->
            DriverListItem(
                id = driver.id,
                firstName = driver.firstName,
                lastName = driver.lastName,
                phone = driver.phone,
                email = driver.email,
                status = driver.status,
                licenseCategories = driver.drivingLicense.categories,
                licenseValidUntil = driver.drivingLicense.validUntil,
                medicalCertificateValidUntil = driver.medicalCertificate.validUntil,
                todayRoutesCount = 0
            )
        }
    }
}