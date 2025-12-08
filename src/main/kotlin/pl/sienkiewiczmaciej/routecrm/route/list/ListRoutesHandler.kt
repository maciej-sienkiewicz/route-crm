package pl.sienkiewiczmaciej.routecrm.route.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.LocalDate
import java.time.LocalTime

data class ListRoutesQuery(
    val companyId: CompanyId,
    val date: LocalDate?,
    val status: RouteStatus?,
    val driverId: DriverId?,
    val pageable: Pageable
)

data class RouteListItem(
    val id: RouteId,
    val routeName: String,
    val date: LocalDate,
    val status: RouteStatus,
    val driverId: DriverId?,
    val driverFirstName: String,
    val driverLastName: String,
    val vehicleId: VehicleId,
    val vehicleRegistrationNumber: String,
    val vehicleModel: String,
    val estimatedStartTime: LocalTime,
    val estimatedEndTime: LocalTime,
    val stopsCount: Int,
    val isDelayed: Boolean,
    val delayMinutes: Int?
)

@Component
class ListRoutesHandler(
    private val routeRepository: RouteRepository,
    private val stopRepository: RouteStopRepository,
    private val driverRepository: DriverJpaRepository,
    private val vehicleRepository: VehicleJpaRepository,
    private val delayEventRepository: RouteDelayEventRepository,
    private val authService: AuthorizationService
) {
    @Transactional(readOnly = true)
    suspend fun handle(principal: UserPrincipal, query: ListRoutesQuery): Page<RouteListItem> {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR, UserRole.DRIVER, UserRole.GUARDIAN)
        authService.requireSameCompany(principal.companyId, query.companyId)

        val routes = when (principal.role) {
            UserRole.DRIVER -> {
                require(principal.driverId != null) { "Driver ID is required" }
                routeRepository.findByDriver(
                    companyId = query.companyId,
                    driverId = DriverId.from(principal.driverId),
                    date = query.date,
                    pageable = query.pageable
                )
            }
            UserRole.GUARDIAN -> {
                routeRepository.findAll(
                    companyId = query.companyId,
                    date = query.date,
                    status = query.status,
                    driverId = query.driverId,
                    pageable = query.pageable
                )
            }
            else -> {
                routeRepository.findAll(
                    companyId = query.companyId,
                    date = query.date,
                    status = query.status,
                    driverId = query.driverId,
                    pageable = query.pageable
                )
            }
        }

        val items = enrichRouteListItems(routes, query.companyId)

        return PageImpl(items, routes.pageable, routes.totalElements)
    }

    private suspend fun enrichRouteListItems(
        routes: Page<pl.sienkiewiczmaciej.routecrm.route.domain.Route>,
        companyId: CompanyId
    ): List<RouteListItem> = withContext(Dispatchers.IO) {
        routes.content.map { route ->
            async {
                val driver = route.driverId?.let { driverId ->
                    driverRepository.findByIdAndCompanyId(
                        driverId.value,
                        companyId.value
                    )
                }

                val vehicle = vehicleRepository.findByIdAndCompanyId(
                    route.vehicleId.value,
                    companyId.value
                )

                val stopsCount = stopRepository.countByRoute(companyId, route.id)

                val delayEvents = delayEventRepository.findByRoute(companyId, route.id)
                val isDelayed = delayEvents.isNotEmpty()
                val maxDelayMinutes = if (isDelayed) {
                    delayEvents.maxOfOrNull { it.delayMinutes }
                } else {
                    null
                }

                RouteListItem(
                    id = route.id,
                    routeName = route.routeName,
                    date = route.date,
                    status = route.status,
                    driverId = route.driverId,
                    driverFirstName = driver?.firstName ?: "",
                    driverLastName = driver?.lastName ?: "",
                    vehicleId = route.vehicleId,
                    vehicleRegistrationNumber = vehicle?.registrationNumber ?: "",
                    vehicleModel = vehicle?.model ?: "",
                    estimatedStartTime = route.estimatedStartTime,
                    estimatedEndTime = route.estimatedEndTime,
                    stopsCount = stopsCount,
                    isDelayed = isDelayed,
                    delayMinutes = maxDelayMinutes
                )
            }
        }.awaitAll()
    }
}