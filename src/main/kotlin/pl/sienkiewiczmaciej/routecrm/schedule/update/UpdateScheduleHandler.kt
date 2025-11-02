package pl.sienkiewiczmaciej.routecrm.schedule.update

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.schedule.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.getbyid.ScheduleNotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.external.GeocodingService
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class UpdateScheduleCommand(
    val companyId: CompanyId,
    val id: ScheduleId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddressData: Address,
    val pickupAddressLabel: String?,
    val dropoffTime: LocalTime,
    val dropoffAddressData: Address,
    val dropoffAddressLabel: String?,
    val specialInstructions: String?,
    val active: Boolean
)

data class UpdateScheduleResult(
    val id: ScheduleId,
    val name: String
)

@Component
class UpdateScheduleHandler(
    private val scheduleRepository: ScheduleRepository,
    private val geocodingService: GeocodingService,
    private val authService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(UpdateScheduleHandler::class.java)

    @Transactional
    suspend fun handle(principal: UserPrincipal, command: UpdateScheduleCommand): UpdateScheduleResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val schedule = scheduleRepository.findById(command.companyId, command.id)
            ?: throw ScheduleNotFoundException(command.id)

        // Sprawdź czy adresy się zmieniły i czy potrzeba nowego geokodowania
        val pickupAddressChanged = schedule.pickupAddress.address != command.pickupAddressData
        val dropoffAddressChanged = schedule.dropoffAddress.address != command.dropoffAddressData

        // Geokodowanie adresu odbioru (jeśli się zmienił)
        val pickupGeocoding = if (pickupAddressChanged) {
            geocodingService.geocodeAddress(command.pickupAddressData).also {
                if (it == null) {
                    logger.warn("Failed to geocode updated pickup address for schedule: ${command.id.value}")
                }
            }
        } else {
            null
        }

        // Geokodowanie adresu dostawy (jeśli się zmienił)
        val dropoffGeocoding = if (dropoffAddressChanged) {
            geocodingService.geocodeAddress(command.dropoffAddressData).also {
                if (it == null) {
                    logger.warn("Failed to geocode updated dropoff address for schedule: ${command.id.value}")
                }
            }
        } else {
            null
        }

        val pickupAddress = ScheduleAddress(
            label = command.pickupAddressLabel,
            address = command.pickupAddressData,
            latitude = if (pickupAddressChanged) pickupGeocoding?.latitude else schedule.pickupAddress.latitude,
            longitude = if (pickupAddressChanged) pickupGeocoding?.longitude else schedule.pickupAddress.longitude
        )

        val dropoffAddress = ScheduleAddress(
            label = command.dropoffAddressLabel,
            address = command.dropoffAddressData,
            latitude = if (dropoffAddressChanged) dropoffGeocoding?.latitude else schedule.dropoffAddress.latitude,
            longitude = if (dropoffAddressChanged) dropoffGeocoding?.longitude else schedule.dropoffAddress.longitude
        )

        val updated = schedule.update(
            name = command.name,
            days = command.days,
            pickupTime = command.pickupTime,
            pickupAddress = pickupAddress,
            dropoffTime = command.dropoffTime,
            dropoffAddress = dropoffAddress,
            specialInstructions = command.specialInstructions,
            active = command.active
        )

        val saved = scheduleRepository.save(updated)

        return UpdateScheduleResult(
            id = saved.id,
            name = saved.name
        )
    }
}