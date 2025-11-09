// src/test/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/services/RouteConflictCheckerTest.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.junit.jupiter.api.Test
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.domain.DrivingLicense
import pl.sienkiewiczmaciej.routecrm.driver.domain.MedicalCertificate
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RouteConflictCheckerTest {
    private val checker = RouteConflictChecker()
    private val testDate = LocalDate.of(2025, 1, 15)

    @Test
    fun `should detect no conflict when no existing routes`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(10, 0))

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = emptyList()
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should detect no conflict when routes do not overlap`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(14, 0), LocalTime.of(16, 0))

        val existingRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(existingRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should detect conflict when routes overlap`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val existingRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(existingRoute)
        )

        assertIs<ConflictCheckResult.HasConflict>(result)
        assertEquals(existingRoute.id, result.conflictingRoute.id)
    }

    @Test
    fun `should detect conflict when new route completely contains existing route`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(7, 0), LocalTime.of(11, 0))

        val existingRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(existingRoute)
        )

        assertIs<ConflictCheckResult.HasConflict>(result)
    }

    @Test
    fun `should detect conflict when existing route completely contains new route`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(8, 30), LocalTime.of(9, 30))

        val existingRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(existingRoute)
        )

        assertIs<ConflictCheckResult.HasConflict>(result)
    }

    @Test
    fun `should detect no conflict when routes are exactly adjacent`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0))

        val existingRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(existingRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should ignore cancelled routes when checking conflicts`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val cancelledRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0),
            status = RouteStatus.CANCELLED
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(cancelledRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should ignore completed routes when checking conflicts`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val completedRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0),
            status = RouteStatus.COMPLETED
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(completedRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should detect conflict with in-progress route`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val inProgressRoute = createRoute(
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0),
            status = RouteStatus.IN_PROGRESS
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(inProgressRoute)
        )

        assertIs<ConflictCheckResult.HasConflict>(result)
    }

    @Test
    fun `should return first conflicting route when multiple conflicts exist`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val route1 = createRoute(
            id = "RT-1",
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val route2 = createRoute(
            id = "RT-2",
            driverId = driver.id,
            date = testDate,
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(12, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(route1, route2)
        )

        assertIs<ConflictCheckResult.HasConflict>(result)
        assertEquals(route1.id, result.conflictingRoute.id)
    }

    @Test
    fun `should detect vehicle conflict when vehicle is already assigned`() {
        val vehicle = createVehicle()
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val existingRoute = createRoute(
            vehicleId = vehicle.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasVehicleConflict(
            vehicle = vehicle,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(existingRoute)
        )

        assertIs<ConflictCheckResult.HasConflict>(result)
    }

    @Test
    fun `should detect no vehicle conflict when times do not overlap`() {
        val vehicle = createVehicle()
        val timeRange = TimeRange(LocalTime.of(14, 0), LocalTime.of(16, 0))

        val existingRoute = createRoute(
            vehicleId = vehicle.id,
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasVehicleConflict(
            vehicle = vehicle,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(existingRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should ignore routes for different drivers when checking driver conflict`() {
        val driver = createDriver(id = "DRV-1")
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val otherDriverRoute = createRoute(
            driverId = DriverId("DRV-2"),
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(otherDriverRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should ignore routes for different vehicles when checking vehicle conflict`() {
        val vehicle = createVehicle(id = "VEH-1")
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val otherVehicleRoute = createRoute(
            vehicleId = VehicleId("VEH-2"),
            date = testDate,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasVehicleConflict(
            vehicle = vehicle,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(otherVehicleRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    @Test
    fun `should ignore routes on different dates`() {
        val driver = createDriver()
        val timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0))

        val differentDateRoute = createRoute(
            driverId = driver.id,
            date = testDate.plusDays(1),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0)
        )

        val result = checker.hasDriverConflict(
            driver = driver,
            date = testDate,
            timeRange = timeRange,
            existingRoutes = listOf(differentDateRoute)
        )

        assertIs<ConflictCheckResult.NoConflict>(result)
    }

    // Helper functions

    private fun createDriver(
        id: String = "DRV-1",
        status: DriverStatus = DriverStatus.ACTIVE
    ) = Driver(
        id = DriverId(id),
        companyId = CompanyId("COMP-1"),
        firstName = "John",
        lastName = "Doe",
        phone = "123456789",
        email = "john@test.com",
        status = status,
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

    private fun createVehicle(
        id: String = "VEH-1"
    ) = Vehicle(
        id = VehicleId(id),
        companyId = CompanyId("COMP-1"),
        registrationNumber = "ABC123",
        model = "Test Van",
        capacity = VehicleCapacity(10, 2, 5),
        status = VehicleStatus.AVAILABLE,
        make = "make",
        year = 2000,
        vehicleType = VehicleType.BUS,
        specialEquipment = emptySet(),
        insurance = null,
        technicalInspection = null,
        currentMileage = 0,
        vin = "empty",
    )

    private fun createRoute(
        id: String = "RT-1",
        driverId: DriverId = DriverId("DRV-1"),
        vehicleId: VehicleId = VehicleId("VEH-1"),
        date: LocalDate = testDate,
        startTime: LocalTime,
        endTime: LocalTime,
        status: RouteStatus = RouteStatus.PLANNED
    ) = Route(
        id = RouteId(id),
        companyId = CompanyId("COMP-1"),
        routeName = "Test Route",
        date = date,
        status = status,
        driverId = driverId,
        vehicleId = vehicleId,
        estimatedStartTime = startTime,
        estimatedEndTime = endTime,
        actualStartTime = null,
        actualEndTime = null
    )
}