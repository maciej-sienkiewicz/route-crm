// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/addschedule/AddRouteScheduleStopsFactory.kt
package pl.sienkiewiczmaciej.routecrm.route.addschedule

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.Child
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.domain.DrivingLicense
import pl.sienkiewiczmaciej.routecrm.driver.domain.MedicalCertificate
import pl.sienkiewiczmaciej.routecrm.route.create.CreateRouteValidationContext
import pl.sienkiewiczmaciej.routecrm.route.create.RouteStopData
import pl.sienkiewiczmaciej.routecrm.route.create.RouteStopFactory
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Vehicle
import java.time.LocalDate

/**
 * Factory responsible for creating pickup and dropoff stops when adding a schedule to a route.
 * Reuses RouteStopFactory for individual stop creation.
 */
@Component
class AddRouteScheduleStopsFactory(
    private val stopFactory: RouteStopFactory
) {

    /**
     * Creates both pickup and dropoff stops for a schedule being added to a route.
     *
     * @param routeId The ID of the route
     * @param companyId The company ID
     * @param command The add schedule command containing stop data
     * @param child The child being added to the route
     * @param schedule The schedule containing address information
     * @param vehicle The vehicle (for validation context)
     * @return Pair of (pickupStop, dropoffStop)
     */
    suspend fun createScheduleStops(
        routeId: RouteId,
        companyId: CompanyId,
        command: AddRouteScheduleCommand,
        child: Child,
        schedule: Schedule,
        vehicle: Vehicle
    ): Pair<RouteStop, RouteStop> {
        // Convert command data to RouteStopData format
        val pickupStopData = RouteStopData(
            stopOrder = command.pickupStop.stopOrder,
            stopType = StopType.PICKUP,
            childId = command.childId,
            scheduleId = command.scheduleId,
            estimatedTime = command.pickupStop.estimatedTime,
            address = command.pickupStop.address
        )

        val dropoffStopData = RouteStopData(
            stopOrder = command.dropoffStop.stopOrder,
            stopType = StopType.DROPOFF,
            childId = command.childId,
            scheduleId = command.scheduleId,
            estimatedTime = command.dropoffStop.estimatedTime,
            address = command.dropoffStop.address
        )

        // Create validation context for stop factory
        val context = CreateRouteValidationContext(
            driver = createDummyDriver(), // Not needed for stop creation
            vehicle = vehicle,
            children = mapOf(child.id to child),
            schedules = mapOf(schedule.id to schedule),
            existingDriverRoutes = emptyList(),
            existingVehicleRoutes = emptyList()
        )

        // Create both stops using the factory
        val stops = stopFactory.createStops(
            routeId = routeId,
            companyId = companyId,
            stopsData = listOf(pickupStopData, dropoffStopData),
            context = context
        )

        return stops[0] to stops[1]
    }

    /**
     * Helper method to create a dummy driver for context.
     * Driver is not actually needed for stop creation, only vehicle and schedules matter.
     */
    private fun createDummyDriver() = Driver(
        id = DriverId("dummy"),
        companyId = CompanyId("dummy"),
        firstName = "Dummy",
        lastName = "Driver",
        phone = "000",
        email = "dummy@example.com",
        status = DriverStatus.ACTIVE,
        dateOfBirth = LocalDate.now(),
        address = Address(
            street = "Street 1",
            houseNumber = "14",
            apartmentNumber = null,
            postalCode = "64-761",
            city = "Poznan"
        ),
        drivingLicense = DrivingLicense(
            licenseNumber = "NO",
            categories = setOf("D"),
            validUntil = LocalDate.now().plusYears(2)
        ),
        medicalCertificate = MedicalCertificate(
            validUntil = LocalDate.now().plusYears(2),
            issueDate = LocalDate.now().minusYears(2)
        ),
    )
}