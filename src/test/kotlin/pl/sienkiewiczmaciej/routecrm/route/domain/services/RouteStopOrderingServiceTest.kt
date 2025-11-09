// src/test/kotlin/pl/sienkiewiczmaciej/routecrm/route/domain/services/RouteStopOrderingServiceTest.kt
package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.route.domain.*
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteStopOrderingServiceTest {
    private val service = RouteStopOrderingService()

    @Test
    fun `insertStopsAt should shift stops at or after insert position`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2),
            createStop(id = "ST-3", order = 3),
            createStop(id = "ST-4", order = 4)
        )

        val result = service.insertStopsAt(
            existingStops = existingStops,
            insertPosition = 2,
            numberOfStopsToInsert = 2
        )

        assertEquals(4, result.size)
        assertEquals(1, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(4, result.find { it.id.value == "ST-2" }?.stopOrder)
        assertEquals(5, result.find { it.id.value == "ST-3" }?.stopOrder)
        assertEquals(6, result.find { it.id.value == "ST-4" }?.stopOrder)
    }

    @Test
    fun `insertStopsAt should not modify stops before insert position`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2),
            createStop(id = "ST-3", order = 3)
        )

        val result = service.insertStopsAt(
            existingStops = existingStops,
            insertPosition = 3,
            numberOfStopsToInsert = 1
        )

        assertEquals(1, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(2, result.find { it.id.value == "ST-2" }?.stopOrder)
        assertEquals(4, result.find { it.id.value == "ST-3" }?.stopOrder)
    }

    @Test
    fun `insertStopsAt should handle insertion at beginning`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val result = service.insertStopsAt(
            existingStops = existingStops,
            insertPosition = 1,
            numberOfStopsToInsert = 2
        )

        assertEquals(3, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(4, result.find { it.id.value == "ST-2" }?.stopOrder)
    }

    @Test
    fun `insertStopsAt should handle insertion at end`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val result = service.insertStopsAt(
            existingStops = existingStops,
            insertPosition = 3,
            numberOfStopsToInsert = 1
        )

        assertEquals(1, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(2, result.find { it.id.value == "ST-2" }?.stopOrder)
    }

    @Test
    fun `insertStopsAt should reject non-positive insert position`() {
        val existingStops = listOf(createStop(order = 1))

        assertThrows<IllegalArgumentException> {
            service.insertStopsAt(existingStops, insertPosition = 0, numberOfStopsToInsert = 1)
        }
    }

    @Test
    fun `insertStopsAt should reject non-positive number of stops`() {
        val existingStops = listOf(createStop(order = 1))

        assertThrows<IllegalArgumentException> {
            service.insertStopsAt(existingStops, insertPosition = 1, numberOfStopsToInsert = 0)
        }
    }

    @Test
    fun `removeStopsAndReorder should remove specified stops and renumber`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2),
            createStop(id = "ST-3", order = 3),
            createStop(id = "ST-4", order = 4),
            createStop(id = "ST-5", order = 5)
        )

        val result = service.removeStopsAndReorder(
            existingStops = existingStops,
            stopsToRemove = setOf(RouteStopId("ST-2"), RouteStopId("ST-4"))
        )

        assertEquals(3, result.size)
        assertEquals(1, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(2, result.find { it.id.value == "ST-3" }?.stopOrder)
        assertEquals(3, result.find { it.id.value == "ST-5" }?.stopOrder)
    }

    @Test
    fun `removeStopsAndReorder should handle removing first stop`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2),
            createStop(id = "ST-3", order = 3)
        )

        val result = service.removeStopsAndReorder(
            existingStops = existingStops,
            stopsToRemove = setOf(RouteStopId("ST-1"))
        )

        assertEquals(2, result.size)
        assertEquals(1, result.find { it.id.value == "ST-2" }?.stopOrder)
        assertEquals(2, result.find { it.id.value == "ST-3" }?.stopOrder)
    }

    @Test
    fun `removeStopsAndReorder should handle removing last stop`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2),
            createStop(id = "ST-3", order = 3)
        )

        val result = service.removeStopsAndReorder(
            existingStops = existingStops,
            stopsToRemove = setOf(RouteStopId("ST-3"))
        )

        assertEquals(2, result.size)
        assertEquals(1, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(2, result.find { it.id.value == "ST-2" }?.stopOrder)
    }

    @Test
    fun `removeStopsAndReorder should handle removing all stops`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val result = service.removeStopsAndReorder(
            existingStops = existingStops,
            stopsToRemove = setOf(RouteStopId("ST-1"), RouteStopId("ST-2"))
        )

        assertEquals(0, result.size)
    }

    @Test
    fun `removeStopsAndReorder should return original list when no stops to remove`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val result = service.removeStopsAndReorder(
            existingStops = existingStops,
            stopsToRemove = emptySet()
        )

        assertEquals(2, result.size)
        assertEquals(1, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(2, result.find { it.id.value == "ST-2" }?.stopOrder)
    }

    @Test
    fun `reorderStops should apply custom ordering`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2),
            createStop(id = "ST-3", order = 3),
            createStop(id = "ST-4", order = 4)
        )

        val newOrderMapping = mapOf(
            RouteStopId("ST-1") to 2,
            RouteStopId("ST-2") to 4,
            RouteStopId("ST-3") to 1,
            RouteStopId("ST-4") to 3
        )

        val result = service.reorderStops(existingStops, newOrderMapping)

        assertEquals(2, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(4, result.find { it.id.value == "ST-2" }?.stopOrder)
        assertEquals(1, result.find { it.id.value == "ST-3" }?.stopOrder)
        assertEquals(3, result.find { it.id.value == "ST-4" }?.stopOrder)
    }

    @Test
    fun `reorderStops should reject non-consecutive orders`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val newOrderMapping = mapOf(
            RouteStopId("ST-1") to 1,
            RouteStopId("ST-2") to 3  // Gap in sequence
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.reorderStops(existingStops, newOrderMapping)
        }
        assertTrue(exception.message!!.contains("consecutive"))
    }

    @Test
    fun `reorderStops should reject orders not starting from 1`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val newOrderMapping = mapOf(
            RouteStopId("ST-1") to 2,
            RouteStopId("ST-2") to 3
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.reorderStops(existingStops, newOrderMapping)
        }
        assertTrue(exception.message!!.contains("consecutive"))
    }

    @Test
    fun `reorderStops should reject incomplete mapping`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val newOrderMapping = mapOf(
            RouteStopId("ST-1") to 1
            // Missing ST-2
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.reorderStops(existingStops, newOrderMapping)
        }
        assertTrue(exception.message!!.contains("All stops must have"))
    }

    @Test
    fun `reorderStops should handle identity mapping`() {
        val existingStops = listOf(
            createStop(id = "ST-1", order = 1),
            createStop(id = "ST-2", order = 2)
        )

        val newOrderMapping = mapOf(
            RouteStopId("ST-1") to 1,
            RouteStopId("ST-2") to 2
        )

        val result = service.reorderStops(existingStops, newOrderMapping)

        assertEquals(1, result.find { it.id.value == "ST-1" }?.stopOrder)
        assertEquals(2, result.find { it.id.value == "ST-2" }?.stopOrder)
    }

    @Test
    fun `getNextStopOrder should return 1 for empty list`() {
        val result = service.getNextStopOrder(emptyList())
        assertEquals(1, result)
    }

    @Test
    fun `getNextStopOrder should return max plus one`() {
        val existingStops = listOf(
            createStop(order = 1),
            createStop(order = 2),
            createStop(order = 3)
        )

        val result = service.getNextStopOrder(existingStops)
        assertEquals(4, result)
    }

    @Test
    fun `getNextStopOrder should work with non-consecutive orders`() {
        val existingStops = listOf(
            createStop(order = 1),
            createStop(order = 5),
            createStop(order = 3)
        )

        val result = service.getNextStopOrder(existingStops)
        assertEquals(6, result)
    }

    @Test
    fun `areStopOrdersValid should return true for consecutive orders`() {
        val stops = listOf(
            createStop(order = 1),
            createStop(order = 2),
            createStop(order = 3)
        )

        assertTrue(service.areStopOrdersValid(stops))
    }

    @Test
    fun `areStopOrdersValid should return true for empty list`() {
        assertTrue(service.areStopOrdersValid(emptyList()))
    }

    @Test
    fun `areStopOrdersValid should return false for non-consecutive orders`() {
        val stops = listOf(
            createStop(order = 1),
            createStop(order = 3),
            createStop(order = 4)
        )

        assertFalse(service.areStopOrdersValid(stops))
    }

    @Test
    fun `areStopOrdersValid should return false for orders not starting from 1`() {
        val stops = listOf(
            createStop(order = 2),
            createStop(order = 3),
            createStop(order = 4)
        )

        assertFalse(service.areStopOrdersValid(stops))
    }

    @Test
    fun `areStopOrdersValid should return false for duplicate orders`() {
        val stops = listOf(
            createStop(order = 1),
            createStop(order = 2),
            createStop(order = 2)
        )

        assertFalse(service.areStopOrdersValid(stops))
    }

    @Test
    fun `areStopOrdersValid should handle unsorted input`() {
        val stops = listOf(
            createStop(order = 3),
            createStop(order = 1),
            createStop(order = 2)
        )

        assertTrue(service.areStopOrdersValid(stops))
    }

    // Helper function

    private fun createStop(
        id: String = "ST-${System.nanoTime()}",
        order: Int
    ) = RouteStop(
        id = RouteStopId(id),
        companyId = CompanyId("COMP-1"),
        routeId = RouteId("RT-1"),
        stopOrder = order,
        stopType = StopType.PICKUP,
        childId = ChildId("CH-1"),
        scheduleId = ScheduleId("SCH-1"),
        estimatedTime = LocalTime.of(8, 0),
        address = ScheduleAddress(
            label = "Home",
            address = Address("Main St", "1", null, "12-345", "Warsaw"),
            latitude = null,
            longitude = null
        ),
        isCancelled = false,
        cancelledAt = null,
        cancellationReason = null,
        actualTime = null,
        executionStatus = null,
        executionNotes = null,
        executedByUserId = null,
        executedByName = null
    )
}