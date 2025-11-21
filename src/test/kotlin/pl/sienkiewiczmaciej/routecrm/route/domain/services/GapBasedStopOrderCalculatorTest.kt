package pl.sienkiewiczmaciej.routecrm.route.domain.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopId
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalTime

@DisplayName("GapBasedStopOrderCalculator")
class GapBasedStopOrderCalculatorTest {

    private lateinit var calculator: GapBasedStopOrderCalculator

    @BeforeEach
    fun setUp() {
        calculator = GapBasedStopOrderCalculator()
    }

    @Nested
    @DisplayName("calculateOrderForInsertion")
    inner class CalculateOrderForInsertionTest {

        @Test
        fun `should return GAP_SIZE when no existing stops`() {
            val result = calculator.calculateOrderForInsertion(
                existingStops = emptyList(),
                afterOrder = null
            )

            assertThat(result.order).isEqualTo(GapBasedStopOrderCalculator.GAP_SIZE)
            assertThat(result.needsRebalancing).isFalse()
        }

        @Test
        fun `should calculate order before first stop when afterOrder is null`() {
            val existingStops = listOf(
                createStop(order = 2000),
                createStop(order = 3000)
            )

            val result = calculator.calculateOrderForInsertion(
                existingStops = existingStops,
                afterOrder = null
            )

            assertThat(result.order).isEqualTo(1000)
            assertThat(result.needsRebalancing).isFalse()
        }

        @Test
        fun `should set needsRebalancing when gap before first stop is too small`() {
            val existingStops = listOf(
                createStop(order = 5),
                createStop(order = 1000)
            )

            val result = calculator.calculateOrderForInsertion(
                existingStops = existingStops,
                afterOrder = null
            )

            assertThat(result.order).isEqualTo(GapBasedStopOrderCalculator.GAP_SIZE)
            assertThat(result.needsRebalancing).isTrue()
        }

        @Test
        fun `should calculate order after last stop when no next stop`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 2000),
                createStop(order = 3000)
            )

            val result = calculator.calculateOrderForInsertion(
                existingStops = existingStops,
                afterOrder = 3000
            )

            assertThat(result.order).isEqualTo(4000)
            assertThat(result.needsRebalancing).isFalse()
        }

        @Test
        fun `should calculate middle position between two stops with large gap`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 2000),
                createStop(order = 4000)
            )

            val result = calculator.calculateOrderForInsertion(
                existingStops = existingStops,
                afterOrder = 2000
            )

            assertThat(result.order).isEqualTo(3000)
            assertThat(result.needsRebalancing).isFalse()
        }

        @Test
        fun `should calculate middle position with odd gap size`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 2000),
                createStop(order = 3001)
            )

            val result = calculator.calculateOrderForInsertion(
                existingStops = existingStops,
                afterOrder = 2000
            )

            assertThat(result.order).isEqualTo(2500)
            assertThat(result.needsRebalancing).isFalse()
        }

        @Test
        fun `should throw InsufficientGapException when gap is too small`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 1005)
            )

            val exception = assertThrows<InsufficientGapException> {
                calculator.calculateOrderForInsertion(
                    existingStops = existingStops,
                    afterOrder = 1000
                )
            }

            assertThat(exception.requiredGap).isEqualTo(GapBasedStopOrderCalculator.MIN_GAP)
            assertThat(exception.availableGap).isEqualTo(5)
        }

        @Test
        fun `should throw exception when afterOrder stop not found`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 2000)
            )

            assertThrows<IllegalArgumentException> {
                calculator.calculateOrderForInsertion(
                    existingStops = existingStops,
                    afterOrder = 999
                )
            }
        }
    }

    @Nested
    @DisplayName("calculateOrdersForMultipleInsertions")
    inner class CalculateOrdersForMultipleInsertionsTest {

        @Test
        fun `should fail when count is zero`() {
            assertThrows<IllegalArgumentException> {
                calculator.calculateOrdersForMultipleInsertions(
                    existingStops = emptyList(),
                    afterOrder = null,
                    count = 0
                )
            }
        }

        @Test
        fun `should fail when count is negative`() {
            assertThrows<IllegalArgumentException> {
                calculator.calculateOrdersForMultipleInsertions(
                    existingStops = emptyList(),
                    afterOrder = null,
                    count = -1
                )
            }
        }

        @Test
        fun `should calculate orders for empty route with multiple stops`() {
            val result = calculator.calculateOrdersForMultipleInsertions(
                existingStops = emptyList(),
                afterOrder = null,
                count = 3
            )

            assertThat(result.orders).containsExactly(1000, 2000, 3000)
            assertThat(result.needsRebalancing).isFalse()
        }

        @Test
        fun `should calculate orders before first stop`() {
            val existingStops = listOf(
                createStop(order = 5000)
            )

            val result = calculator.calculateOrdersForMultipleInsertions(
                existingStops = existingStops,
                afterOrder = null,
                count = 2
            )

            assertThat(result.orders).hasSize(2)
            assertThat(result.orders[0]).isLessThan(result.orders[1])
            assertThat(result.orders[1]).isLessThan(5000)
        }

        @Test
        fun `should throw InsufficientGapException when not enough space before first stop`() {
            val existingStops = listOf(
                createStop(order = 50)
            )

            assertThrows<InsufficientGapException> {
                calculator.calculateOrdersForMultipleInsertions(
                    existingStops = existingStops,
                    afterOrder = null,
                    count = 3
                )
            }
        }

        @Test
        fun `should calculate orders after last stop`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 2000)
            )

            val result = calculator.calculateOrdersForMultipleInsertions(
                existingStops = existingStops,
                afterOrder = 2000,
                count = 3
            )

            assertThat(result.orders).containsExactly(3000, 4000, 5000)
            assertThat(result.needsRebalancing).isFalse()
        }

        @Test
        fun `should calculate evenly distributed orders between stops`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 2000),
                createStop(order = 5000)
            )

            val result = calculator.calculateOrdersForMultipleInsertions(
                existingStops = existingStops,
                afterOrder = 2000,
                count = 2
            )

            assertThat(result.orders).hasSize(2)
            assertThat(result.orders[0]).isGreaterThan(2000)
            assertThat(result.orders[0]).isLessThan(result.orders[1])
            assertThat(result.orders[1]).isLessThan(5000)
        }

        @Test
        fun `should throw InsufficientGapException when gap between stops is too small`() {
            val existingStops = listOf(
                createStop(order = 1000),
                createStop(order = 1025)
            )

            assertThrows<InsufficientGapException> {
                calculator.calculateOrdersForMultipleInsertions(
                    existingStops = existingStops,
                    afterOrder = 1000,
                    count = 3
                )
            }
        }

        @Test
        fun `should throw exception when afterOrder stop not found`() {
            val existingStops = listOf(
                createStop(order = 1000)
            )

            assertThrows<IllegalArgumentException> {
                calculator.calculateOrdersForMultipleInsertions(
                    existingStops = existingStops,
                    afterOrder = 999,
                    count = 2
                )
            }
        }
    }

    @Nested
    @DisplayName("needsRebalancing")
    inner class NeedsRebalancingTest {

        @Test
        fun `should return false for empty list`() {
            val result = calculator.needsRebalancing(emptyList())
            assertThat(result).isFalse()
        }

        @Test
        fun `should return false for single stop`() {
            val stops = listOf(createStop(order = 1000))
            val result = calculator.needsRebalancing(stops)
            assertThat(result).isFalse()
        }

        @Test
        fun `should return false when compression ratio is above threshold`() {
            val stops = listOf(
                createStop(order = 1000),
                createStop(order = 2000),
                createStop(order = 3000),
                createStop(order = 4000)
            )

            val result = calculator.needsRebalancing(stops)
            assertThat(result).isFalse()
        }

        @Test
        fun `should return true when compression ratio is below threshold`() {
            val stops = listOf(
                createStop(order = 1000),
                createStop(order = 1100),
                createStop(order = 1200),
                createStop(order = 1300)
            )

            val result = calculator.needsRebalancing(stops)
            assertThat(result).isTrue()
        }

        @Test
        fun `should return true when stops are highly compressed`() {
            val stops = listOf(
                createStop(order = 1000),
                createStop(order = 1001),
                createStop(order = 1002),
                createStop(order = 1003)
            )

            val result = calculator.needsRebalancing(stops)
            assertThat(result).isTrue()
        }

        @Test
        fun `should handle unsorted stops correctly`() {
            val stops = listOf(
                createStop(order = 3000),
                createStop(order = 1000),
                createStop(order = 2000)
            )

            val result = calculator.needsRebalancing(stops)
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("rebalance")
    inner class RebalanceTest {

        @Test
        fun `should return empty list for empty input`() {
            val result = calculator.rebalance(emptyList())
            assertThat(result).isEmpty()
        }

        @Test
        fun `should rebalance single stop to GAP_SIZE`() {
            val stops = listOf(createStop(order = 555))

            val result = calculator.rebalance(stops)

            assertThat(result).hasSize(1)
            assertThat(result[0].stopOrder).isEqualTo(GapBasedStopOrderCalculator.GAP_SIZE)
        }

        @Test
        fun `should rebalance compressed stops to GAP_SIZE intervals`() {
            val stops = listOf(
                createStop(order = 1001),
                createStop(order = 1002),
                createStop(order = 1003),
                createStop(order = 1004)
            )

            val result = calculator.rebalance(stops)

            assertThat(result).hasSize(4)
            assertThat(result[0].stopOrder).isEqualTo(1000)
            assertThat(result[1].stopOrder).isEqualTo(2000)
            assertThat(result[2].stopOrder).isEqualTo(3000)
            assertThat(result[3].stopOrder).isEqualTo(4000)
        }

        @Test
        fun `should rebalance unsorted stops maintaining logical order`() {
            val stops = listOf(
                createStop(id = "1", order = 500),
                createStop(id = "2", order = 3000),
                createStop(id = "3", order = 100)
            )

            val result = calculator.rebalance(stops)

            assertThat(result).hasSize(3)
            assertThat(result[0].id.value).isEqualTo("3")
            assertThat(result[0].stopOrder).isEqualTo(1000)
            assertThat(result[1].id.value).isEqualTo("1")
            assertThat(result[1].stopOrder).isEqualTo(2000)
            assertThat(result[2].id.value).isEqualTo("2")
            assertThat(result[2].stopOrder).isEqualTo(3000)
        }

        @Test
        fun `should maintain other stop properties after rebalancing`() {
            val originalStop = createStop(
                id = "test-123",
                order = 555,
                childId = "child-456",
                scheduleId = "schedule-789"
            )

            val result = calculator.rebalance(listOf(originalStop))

            assertThat(result).hasSize(1)
            assertThat(result[0].id.value).isEqualTo("test-123")
            assertThat(result[0].childId.value).isEqualTo("child-456")
            assertThat(result[0].scheduleId.value).isEqualTo("schedule-789")
            assertThat(result[0].stopOrder).isEqualTo(GapBasedStopOrderCalculator.GAP_SIZE)
        }

        @Test
        fun `should rebalance large number of stops`() {
            val stops = (1..100).map { createStop(order = it) }

            val result = calculator.rebalance(stops)

            assertThat(result).hasSize(100)
            assertThat(result[0].stopOrder).isEqualTo(1000)
            assertThat(result[99].stopOrder).isEqualTo(100000)

            for (i in 1 until result.size) {
                val gap = result[i].stopOrder - result[i - 1].stopOrder
                assertThat(gap).isEqualTo(GapBasedStopOrderCalculator.GAP_SIZE)
            }
        }
    }

    private fun createStop(
        id: String = "ST-${java.util.UUID.randomUUID()}",
        order: Int,
        childId: String = "child-123",
        scheduleId: String = "schedule-456"
    ): RouteStop {
        return RouteStop(
            id = RouteStopId(id),
            companyId = CompanyId("test-company"),
            routeId = RouteId("test-route"),
            stopOrder = order,
            stopType = StopType.PICKUP,
            childId = ChildId(childId),
            scheduleId = ScheduleId(scheduleId),
            estimatedTime = LocalTime.of(8, 0),
            address = ScheduleAddress(
                label = "Test",
                address = Address(
                    street = "Test St",
                    houseNumber = "1",
                    apartmentNumber = null,
                    postalCode = "00-000",
                    city = "Test City"
                ),
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
}