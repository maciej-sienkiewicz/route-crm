package pl.sienkiewiczmaciej.routecrm.schedule.create

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildRepository
import pl.sienkiewiczmaciej.routecrm.child.getbyid.ChildNotFoundException
import pl.sienkiewiczmaciej.routecrm.schedule.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserPrincipal
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.shared.external.GeocodingService
import pl.sienkiewiczmaciej.routecrm.shared.infrastructure.security.AuthorizationService
import java.time.LocalTime

data class CreateScheduleCommand(
    val companyId: CompanyId,
    val childId: ChildId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddressData: Address,
    val pickupAddressLabel: String?,
    val dropoffTime: LocalTime,
    val dropoffAddressData: Address,
    val dropoffAddressLabel: String?,
    val specialInstructions: String?
)

data class CreateScheduleResult(
    val id: ScheduleId,
    val childId: ChildId,
    val companyId: CompanyId,
    val name: String,
    val days: Set<DayOfWeek>,
    val pickupTime: LocalTime,
    val pickupAddress: ScheduleAddress,      // ← ZMIANA: zwracamy pełny ScheduleAddress
    val dropoffTime: LocalTime,
    val dropoffAddress: ScheduleAddress,     // ← ZMIANA: zwracamy pełny ScheduleAddress
    val specialInstructions: String?,
    val active: Boolean
)

@Component
class CreateScheduleHandler(
    private val scheduleRepository: ScheduleRepository,
    private val childRepository: ChildRepository,
    private val geocodingService: GeocodingService,
    private val authService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(CreateScheduleHandler::class.java)

    @Transactional
    suspend fun handle(principal: UserPrincipal, command: CreateScheduleCommand): CreateScheduleResult {
        authService.requireRole(principal, UserRole.ADMIN, UserRole.OPERATOR)
        authService.requireSameCompany(principal.companyId, command.companyId)

        val child = childRepository.findById(command.companyId, command.childId)
            ?: throw ChildNotFoundException(command.childId)

        // Geokodowanie adresu odbioru
        val pickupGeocoding = geocodingService.geocodeAddress(command.pickupAddressData)
        if (pickupGeocoding == null) {
            logger.warn("Failed to geocode pickup address for schedule: ${command.name}")
        }

        // Geokodowanie adresu dostawy
        val dropoffGeocoding = geocodingService.geocodeAddress(command.dropoffAddressData)
        if (dropoffGeocoding == null) {
            logger.warn("Failed to geocode dropoff address for schedule: ${command.name}")
        }

        val pickupAddress = ScheduleAddress(
            label = command.pickupAddressLabel,
            address = command.pickupAddressData,
            latitude = pickupGeocoding?.latitude,
            longitude = pickupGeocoding?.longitude
        )

        val dropoffAddress = ScheduleAddress(
            label = command.dropoffAddressLabel,
            address = command.dropoffAddressData,
            latitude = dropoffGeocoding?.latitude,
            longitude = dropoffGeocoding?.longitude
        )

        val schedule = Schedule.create(
            companyId = command.companyId,
            childId = command.childId,
            name = command.name,
            days = command.days,
            pickupTime = command.pickupTime,
            pickupAddress = pickupAddress,
            dropoffTime = command.dropoffTime,
            dropoffAddress = dropoffAddress,
            specialInstructions = command.specialInstructions
        )

        val saved = scheduleRepository.save(schedule)

        return CreateScheduleResult(
            id = saved.id,
            childId = saved.childId,
            companyId = saved.companyId,
            name = saved.name,
            days = saved.days,
            pickupTime = saved.pickupTime,
            pickupAddress = saved.pickupAddress,      // ← Zwracamy z geokodowaniem
            dropoffTime = saved.dropoffTime,
            dropoffAddress = saved.dropoffAddress,    // ← Zwracamy z geokodowaniem
            specialInstructions = saved.specialInstructions,
            active = saved.active
        )
    }
}