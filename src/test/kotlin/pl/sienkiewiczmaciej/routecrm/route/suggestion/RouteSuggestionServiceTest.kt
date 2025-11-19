package pl.sienkiewiczmaciej.routecrm.route.suggestion

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.domain.services.RouteStops
import pl.sienkiewiczmaciej.routecrm.route.domain.services.RouteSuggestionService
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleAddress
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteSuggestionServiceTest {

    private val service = RouteSuggestionService()
    private val companyId = CompanyId.generate()

    @Nested
    inner class WhenNoRoutesProvided {

        @Test
        fun `should return empty list when routes list is empty`() = runTest {
            // Given
            val emptyRoutes = emptyList<RouteStops>()
            val newSchedule = createSchedule(
                pickupLat = 52.2297,
                pickupLon = 21.0122,
                dropoffLat = 52.2400,
                dropoffLon = 21.0200
            )

            // When
            val suggestions = service.findSuggestions(emptyRoutes, newSchedule)

            // Then
            assertEquals(emptyList(), suggestions)
        }
    }

    @Nested
    inner class WhenRouteHasInsufficientStops {

        @Test
        fun `should skip route when it has no stops`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(routeId = routeId, stops = emptyList())
            )
            val newSchedule = createSchedule(
                pickupLat = 52.2297,
                pickupLon = 21.0122,
                dropoffLat = 52.2400,
                dropoffLon = 21.0200
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertEquals(emptyList(), suggestions)
        }

        @Test
        fun `should skip route when it has only one stop`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.2297, lon = 21.0122)
                    )
                )
            )
            val newSchedule = createSchedule(
                pickupLat = 52.2297,
                pickupLon = 21.0122,
                dropoffLat = 52.2400,
                dropoffLon = 21.0200
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertEquals(emptyList(), suggestions)
        }
    }

    @Nested
    inner class PickupDistanceFiltering {

        @Test
        fun `should reject route when pickup point is too far from route line`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.01, lon = 21.01)
                    )
                )
            )
            // Pickup point very far away (more than 1000m from route)
            val newSchedule = createSchedule(
                pickupLat = 52.5,
                pickupLon = 21.5,
                dropoffLat = 52.51,
                dropoffLon = 21.51
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertEquals(emptyList(), suggestions)
        }

        @Test
        fun `should accept route when pickup point is within max coverage distance`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.2297, lon = 21.0122),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.2400, lon = 21.0200),
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2500, lon = 21.0300)
                    )
                )
            )
            // Pickup point very close to the route
            val newSchedule = createSchedule(
                pickupLat = 52.2300,
                pickupLon = 21.0125,
                dropoffLat = 52.2450,
                dropoffLon = 21.0250
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }

        @Test
        fun `should accept route when pickup point is exactly on the route line`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.1, lon = 21.1)
                    )
                )
            )
            // Pickup point exactly on the route line
            val newSchedule = createSchedule(
                pickupLat = 52.05,
                pickupLon = 21.05,
                dropoffLat = 52.08,
                dropoffLon = 21.08
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }
    }

    @Nested
    inner class RemainingRouteCalculation {

        @Test
        fun `should suggest route when remaining route has less than 2 points after pickup`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.1, lon = 21.1)
                    )
                )
            )
            // Pickup very close to the last stop (within 1000m)
            val newSchedule = createSchedule(
                pickupLat = 52.099,
                pickupLon = 21.099,
                dropoffLat = 52.2,
                dropoffLon = 21.2
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId), "Route should be suggested when pickup is near end")
        }

        @Test
        fun `should correctly calculate remaining route from pickup point index`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.1, lon = 21.1),
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2, lon = 21.2),
                        createRouteStop(routeId, stopOrder = 4, lat = 52.3, lon = 21.3)
                    )
                )
            )
            // Pickup near second stop, dropoff near third stop
            val newSchedule = createSchedule(
                pickupLat = 52.1005,
                pickupLon = 21.1005,
                dropoffLat = 52.2005,
                dropoffLon = 21.2005
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }
    }

    @Nested
    inner class DropoffDistanceFiltering {

        @Test
        fun `should reject route when dropoff is too far from remaining route`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.1, lon = 21.1),
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2, lon = 21.2)
                    )
                )
            )
            // Pickup near first stop, but dropoff very far away
            val newSchedule = createSchedule(
                pickupLat = 52.001,
                pickupLon = 21.001,
                dropoffLat = 53.0,
                dropoffLon = 22.0
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertEquals(emptyList(), suggestions)
        }

        @Test
        fun `should accept route when dropoff is within max coverage distance of remaining route`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.1, lon = 21.1),
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2, lon = 21.2)
                    )
                )
            )
            // Both pickup and dropoff close to the route
            val newSchedule = createSchedule(
                pickupLat = 52.05,
                pickupLon = 21.05,
                dropoffLat = 52.15,
                dropoffLon = 21.15
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }

        @Test
        fun `should accept route when dropoff is exactly on remaining route line`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.1, lon = 21.1),
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2, lon = 21.2)
                    )
                )
            )
            // Dropoff exactly on the route between second and third stop
            val newSchedule = createSchedule(
                pickupLat = 52.05,
                pickupLon = 21.05,
                dropoffLat = 52.15,
                dropoffLon = 21.15
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }
    }

    @Nested
    inner class MultipleRoutesScenarios {

        @Test
        fun `should return multiple suggestions when multiple routes match criteria`() = runTest {
            // Given
            val routeId1 = RouteId.generate()
            val routeId2 = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId1,
                    stops = listOf(
                        createRouteStop(routeId1, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId1, stopOrder = 2, lat = 52.1, lon = 21.1),
                        createRouteStop(routeId1, stopOrder = 3, lat = 52.2, lon = 21.2)
                    )
                ),
                RouteStops(
                    routeId = routeId2,
                    stops = listOf(
                        createRouteStop(routeId2, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId2, stopOrder = 2, lat = 52.15, lon = 21.15),
                        createRouteStop(routeId2, stopOrder = 3, lat = 52.3, lon = 21.3)
                    )
                )
            )
            val newSchedule = createSchedule(
                pickupLat = 52.05,
                pickupLon = 21.05,
                dropoffLat = 52.15,
                dropoffLon = 21.15
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertEquals(2, suggestions.size)
            assertTrue(suggestions.contains(routeId1))
            assertTrue(suggestions.contains(routeId2))
        }

        @Test
        fun `should filter out routes that don't match while keeping matching ones`() = runTest {
            // Given
            val matchingRouteId = RouteId.generate()
            val nonMatchingRouteId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = matchingRouteId,
                    stops = listOf(
                        createRouteStop(matchingRouteId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(matchingRouteId, stopOrder = 2, lat = 52.1, lon = 21.1),
                        createRouteStop(matchingRouteId, stopOrder = 3, lat = 52.2, lon = 21.2)
                    )
                ),
                RouteStops(
                    routeId = nonMatchingRouteId,
                    stops = listOf(
                        createRouteStop(nonMatchingRouteId, stopOrder = 1, lat = 53.0, lon = 22.0),
                        createRouteStop(nonMatchingRouteId, stopOrder = 2, lat = 53.1, lon = 22.1)
                    )
                )
            )
            val newSchedule = createSchedule(
                pickupLat = 52.05,
                pickupLon = 21.05,
                dropoffLat = 52.15,
                dropoffLon = 21.15
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertEquals(1, suggestions.size)
            assertTrue(suggestions.contains(matchingRouteId))
            assertTrue(!suggestions.contains(nonMatchingRouteId))
        }

        @Test
        fun `should return empty list when no routes match criteria`() = runTest {
            // Given
            val routeId1 = RouteId.generate()
            val routeId2 = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId1,
                    stops = listOf(
                        createRouteStop(routeId1, stopOrder = 1, lat = 53.0, lon = 22.0),
                        createRouteStop(routeId1, stopOrder = 2, lat = 53.1, lon = 22.1)
                    )
                ),
                RouteStops(
                    routeId = routeId2,
                    stops = listOf(
                        createRouteStop(routeId2, stopOrder = 1, lat = 54.0, lon = 23.0),
                        createRouteStop(routeId2, stopOrder = 2, lat = 54.1, lon = 23.1)
                    )
                )
            )
            val newSchedule = createSchedule(
                pickupLat = 52.0,
                pickupLon = 21.0,
                dropoffLat = 52.1,
                dropoffLon = 21.1
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertEquals(emptyList(), suggestions)
        }
    }

    @Nested
    inner class StopOrderHandling {

        @Test
        fun `should correctly sort stops by stopOrder before processing`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2, lon = 21.2),
                        createRouteStop(routeId, stopOrder = 1, lat = 52.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 52.1, lon = 21.1)
                    )
                )
            )
            val newSchedule = createSchedule(
                pickupLat = 52.05,
                pickupLon = 21.05,
                dropoffLat = 52.15,
                dropoffLon = 21.15
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }
    }

    @Nested
    inner class EdgeCasesWithCoordinates {

        @Test
        fun `should handle route with coordinates at equator`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 0.0, lon = 21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = 0.01, lon = 21.01)
                    )
                )
            )
            val newSchedule = createSchedule(
                pickupLat = 0.005,
                pickupLon = 21.005,
                dropoffLat = 0.008,
                dropoffLon = 21.008
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }

        @Test
        fun `should handle route with negative coordinates`() = runTest {
            // Given
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = -52.0, lon = -21.0),
                        createRouteStop(routeId, stopOrder = 2, lat = -52.1, lon = -21.1),
                        createRouteStop(routeId, stopOrder = 3, lat = -52.2, lon = -21.2)
                    )
                )
            )
            val newSchedule = createSchedule(
                pickupLat = -52.05,
                pickupLon = -21.05,
                dropoffLat = -52.15,
                dropoffLon = -21.15
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }
    }

    @Nested
    inner class ComplexRealWorldScenarios {

        @Test
        fun `should handle school bus route with multiple pickups and dropoffs`() = runTest {
            // Given: School bus picking up kids from multiple neighborhoods
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.2297, lon = 21.0122), // Neighborhood 1
                        createRouteStop(routeId, stopOrder = 2, lat = 52.2350, lon = 21.0180), // Neighborhood 2
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2400, lon = 21.0250), // Neighborhood 3
                        createRouteStop(routeId, stopOrder = 4, lat = 52.2500, lon = 21.0350), // School
                    )
                )
            )
            // New student living between neighborhood 2 and 3
            val newSchedule = createSchedule(
                pickupLat = 52.2375,
                pickupLon = 21.0215,
                dropoffLat = 52.2490,
                dropoffLon = 21.0340
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }

        @Test
        fun `should handle courier delivery route in city center`() = runTest {
            // Given: Courier route through city blocks
            val routeId = RouteId.generate()
            val routes = listOf(
                RouteStops(
                    routeId = routeId,
                    stops = listOf(
                        createRouteStop(routeId, stopOrder = 1, lat = 52.2296, lon = 21.0122), // Depot
                        createRouteStop(routeId, stopOrder = 2, lat = 52.2315, lon = 21.0145), // Stop 1
                        createRouteStop(routeId, stopOrder = 3, lat = 52.2335, lon = 21.0168), // Stop 2
                        createRouteStop(routeId, stopOrder = 4, lat = 52.2355, lon = 21.0191), // Stop 3
                        createRouteStop(routeId, stopOrder = 5, lat = 52.2375, lon = 21.0214)  // Stop 4
                    )
                )
            )
            // New delivery between stop 2 and 3
            val newSchedule = createSchedule(
                pickupLat = 52.2325,
                pickupLon = 21.0156,
                dropoffLat = 52.2345,
                dropoffLon = 21.0179
            )

            // When
            val suggestions = service.findSuggestions(routes, newSchedule)

            // Then
            assertTrue(suggestions.contains(routeId))
        }
    }

    // Helper functions

    private fun createSchedule(
        pickupLat: Double,
        pickupLon: Double,
        dropoffLat: Double,
        dropoffLon: Double
    ): Schedule {
        return Schedule.create(
            companyId = companyId,
            childId = ChildId.generate(),
            name = "Test Schedule",
            days = setOf(DayOfWeek.MONDAY),
            pickupTime = LocalTime.of(8, 0),
            pickupAddress = ScheduleAddress(
                label = "Pickup",
                address = Address(
                    street = "Test Street",
                    city = "Warsaw",
                    postalCode = "00-002",
                    houseNumber = "14",
                    apartmentNumber = ""
                ),
                latitude = pickupLat,
                longitude = pickupLon
            ),
            dropoffTime = LocalTime.of(9, 0),
            dropoffAddress = ScheduleAddress(
                label = "Dropoff",
                address = Address(
                    street = "Test Street 2",
                    city = "Warsaw",
                    postalCode = "00-002",
                    houseNumber = "14",
                    apartmentNumber = ""
                ),
                latitude = dropoffLat,
                longitude = dropoffLon
            ),
            specialInstructions = null
        )
    }

    private fun createRouteStop(
        routeId: RouteId,
        stopOrder: Int,
        lat: Double,
        lon: Double
    ): RouteStop {
        return RouteStop.create(
            companyId = companyId,
            routeId = routeId,
            stopOrder = stopOrder,
            stopType = StopType.PICKUP,
            childId = ChildId.generate(),
            scheduleId = ScheduleId.generate(),
            estimatedTime = LocalTime.of(8, 0),
            address = ScheduleAddress(
                label = "Stop $stopOrder",
                address = Address(
                    street = "Test Street $stopOrder",
                    city = "Warsaw",
                    postalCode = "00-00$stopOrder",
                    houseNumber = "14"
                ),
                latitude = lat,
                longitude = lon
            )
        )
    }
}