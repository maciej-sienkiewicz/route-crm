// src/test/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/services/RouteCapacityCalculatorTest.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.junit.jupiter.api.Test
import pl.sienkiewiczmaciej.routecrm.child.domain.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.MedicalCertificate
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleCapacity
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RouteCapacityCalculatorTest {
    private val calculator = RouteCapacityCalculator()

    @Test
    fun `should calculate requirements for children without special needs`() {
        val children = listOf(
            createChild(id = "CH-1", wheelchair = false, specialSeat = false),
            createChild(id = "CH-2", wheelchair = false, specialSeat = false),
            createChild(id = "CH-3", wheelchair = false, specialSeat = false)
        )

        val requirements = calculator.calculateRequirements(children)

        assertEquals(3, requirements.totalSeats)
        assertEquals(0, requirements.wheelchairSpaces)
        assertEquals(0, requirements.childSeats)
    }

    @Test
    fun `should calculate requirements for children with wheelchair needs`() {
        val children = listOf(
            createChild(id = "CH-1", wheelchair = true, specialSeat = false),
            createChild(id = "CH-2", wheelchair = true, specialSeat = false),
            createChild(id = "CH-3", wheelchair = false, specialSeat = false)
        )

        val requirements = calculator.calculateRequirements(children)

        assertEquals(3, requirements.totalSeats)
        assertEquals(2, requirements.wheelchairSpaces)
        assertEquals(0, requirements.childSeats)
    }

    @Test
    fun `should calculate requirements for children with special seat needs`() {
        val children = listOf(
            createChild(id = "CH-1", wheelchair = false, specialSeat = true),
            createChild(id = "CH-2", wheelchair = false, specialSeat = true),
            createChild(id = "CH-3", wheelchair = false, specialSeat = false)
        )

        val requirements = calculator.calculateRequirements(children)

        assertEquals(3, requirements.totalSeats)
        assertEquals(0, requirements.wheelchairSpaces)
        assertEquals(2, requirements.childSeats)
    }

    @Test
    fun `should calculate requirements for children with mixed needs`() {
        val children = listOf(
            createChild(id = "CH-1", wheelchair = true, specialSeat = false),
            createChild(id = "CH-2", wheelchair = false, specialSeat = true),
            createChild(id = "CH-3", wheelchair = false, specialSeat = false)
        )

        val requirements = calculator.calculateRequirements(children)

        assertEquals(3, requirements.totalSeats)
        assertEquals(1, requirements.wheelchairSpaces)
        assertEquals(1, requirements.childSeats)
    }

    @Test
    fun `should count unique children when same child appears multiple times`() {
        val child = createChild(id = "CH-1", wheelchair = true, specialSeat = false)
        val children = listOf(child, child, child)

        val requirements = calculator.calculateRequirements(children)

        assertEquals(1, requirements.totalSeats)
        assertEquals(1, requirements.wheelchairSpaces)
        assertEquals(0, requirements.childSeats)
    }

    @Test
    fun `should return zero requirements for empty list`() {
        val requirements = calculator.calculateRequirements(emptyList())

        assertEquals(0, requirements.totalSeats)
        assertEquals(0, requirements.wheelchairSpaces)
        assertEquals(0, requirements.childSeats)
    }

    @Test
    fun `should return Fits when capacity is sufficient`() {
        val requirements = CapacityRequirements(
            totalSeats = 5,
            wheelchairSpaces = 2,
            childSeats = 3
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 3,
            childSeats = 5
        )

        val result = calculator.checkFits(requirements, vehicleCapacity)

        assertIs<CapacityCheckResult.Fits>(result)
    }

    @Test
    fun `should return Fits when capacity is exactly sufficient`() {
        val requirements = CapacityRequirements(
            totalSeats = 10,
            wheelchairSpaces = 3,
            childSeats = 5
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 3,
            childSeats = 5
        )

        val result = calculator.checkFits(requirements, vehicleCapacity)

        assertIs<CapacityCheckResult.Fits>(result)
    }

    @Test
    fun `should return InsufficientWheelchairSpaces when wheelchair capacity exceeded`() {
        val requirements = CapacityRequirements(
            totalSeats = 5,
            wheelchairSpaces = 3,
            childSeats = 2
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 5
        )

        val result = calculator.checkFits(requirements, vehicleCapacity)

        assertIs<CapacityCheckResult.InsufficientWheelchairSpaces>(result)
        assertEquals(3, result.required)
        assertEquals(2, result.available)
    }

    @Test
    fun `should return InsufficientChildSeats when child seat capacity exceeded`() {
        val requirements = CapacityRequirements(
            totalSeats = 5,
            wheelchairSpaces = 1,
            childSeats = 4
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 3
        )

        val result = calculator.checkFits(requirements, vehicleCapacity)

        assertIs<CapacityCheckResult.InsufficientChildSeats>(result)
        assertEquals(4, result.required)
        assertEquals(3, result.available)
    }

    @Test
    fun `should return InsufficientTotalSeats when total capacity exceeded`() {
        val requirements = CapacityRequirements(
            totalSeats = 12,
            wheelchairSpaces = 1,
            childSeats = 2
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 5
        )

        val result = calculator.checkFits(requirements, vehicleCapacity)

        assertIs<CapacityCheckResult.InsufficientTotalSeats>(result)
        assertEquals(12, result.required)
        assertEquals(10, result.available)
    }

    @Test
    fun `should prioritize wheelchair check over child seat check`() {
        val requirements = CapacityRequirements(
            totalSeats = 5,
            wheelchairSpaces = 3,
            childSeats = 4
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 3
        )

        val result = calculator.checkFits(requirements, vehicleCapacity)

        // Should fail on wheelchair first
        assertIs<CapacityCheckResult.InsufficientWheelchairSpaces>(result)
    }

    @Test
    fun `should prioritize child seat check over total seat check`() {
        val requirements = CapacityRequirements(
            totalSeats = 12,
            wheelchairSpaces = 1,
            childSeats = 6
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 5
        )

        val result = calculator.checkFits(requirements, vehicleCapacity)

        // Should fail on child seats before total seats
        assertIs<CapacityCheckResult.InsufficientChildSeats>(result)
    }

    @Test
    fun `checkChildrenFit should calculate and check in one call`() {
        val children = listOf(
            createChild(id = "CH-1", wheelchair = true, specialSeat = false),
            createChild(id = "CH-2", wheelchair = false, specialSeat = true),
            createChild(id = "CH-3", wheelchair = false, specialSeat = false)
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 3
        )

        val result = calculator.checkChildrenFit(children, vehicleCapacity)

        assertIs<CapacityCheckResult.Fits>(result)
    }

    @Test
    fun `checkChildrenFit should detect insufficient capacity`() {
        val children = listOf(
            createChild(id = "CH-1", wheelchair = true, specialSeat = false),
            createChild(id = "CH-2", wheelchair = true, specialSeat = false),
            createChild(id = "CH-3", wheelchair = true, specialSeat = false)
        )
        val vehicleCapacity = VehicleCapacity(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 5
        )

        val result = calculator.checkChildrenFit(children, vehicleCapacity)

        assertIs<CapacityCheckResult.InsufficientWheelchairSpaces>(result)
    }

    @Test
    fun `should handle child with both wheelchair and special seat needs`() {
        val children = listOf(
            createChild(id = "CH-1", wheelchair = true, specialSeat = true)
        )

        val requirements = calculator.calculateRequirements(children)

        assertEquals(1, requirements.totalSeats)
        assertEquals(1, requirements.wheelchairSpaces)
        assertEquals(1, requirements.childSeats)
    }

    @Test
    fun `should validate CapacityRequirements invariants`() {
        // Should not throw
        CapacityRequirements(totalSeats = 10, wheelchairSpaces = 5, childSeats = 5)
    }

    @Test
    fun `should reject negative values in CapacityRequirements`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CapacityRequirements(totalSeats = -1, wheelchairSpaces = 0, childSeats = 0)
        }
        assertEquals("Total seats cannot be negative", exception.message)
    }

    @Test
    fun `should reject wheelchair spaces exceeding total seats`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CapacityRequirements(totalSeats = 5, wheelchairSpaces = 10, childSeats = 0)
        }
        assertEquals("Wheelchair spaces cannot exceed total seats", exception.message)
    }

    @Test
    fun `should reject child seats exceeding total seats`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            CapacityRequirements(totalSeats = 5, wheelchairSpaces = 0, childSeats = 10)
        }
        assertEquals("Child seats cannot exceed total seats", exception.message)
    }

    // Helper functions

    private fun createChild(
        id: String,
        wheelchair: Boolean,
        specialSeat: Boolean,

    ) = Child(
        id = ChildId(id),
        companyId = CompanyId("COMP-1"),
        firstName = "Child",
        lastName = "Test",
        birthDate = LocalDate.of(2015, 1, 1),
        disability = emptySet(),
        transportNeeds = TransportNeeds(
            wheelchair = wheelchair,
            specialSeat = specialSeat,
            safetyBelt = false,
        ),
        status = ChildStatus.ACTIVE,
        notes = null,
    )
}