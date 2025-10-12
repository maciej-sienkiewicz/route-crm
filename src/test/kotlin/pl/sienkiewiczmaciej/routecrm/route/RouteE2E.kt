package pl.sienkiewiczmaciej.routecrm.route

import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import pl.sienkiewiczmaciej.routecrm.test.BaseE2ETest
import pl.sienkiewiczmaciej.routecrm.test.TestDataFactory
import java.time.Instant
import java.time.LocalDate

class RouteResourceConflictTests : BaseE2ETest() {

    @Test
    fun `should reject route when child already has route at same time`() {
        val driverId = createDriver()
        val vehicle1Id = createVehicle()
        val vehicle2Id = createVehicle(registrationNumber = "WAW 5678B")
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val tomorrow = LocalDate.now().plusDays(1)

        val route1 = createRouteRequest(
            routeName = "Route 1",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicle1Id,
            startTime = "07:30",
            endTime = "09:00",
            children = listOf(
                childRequestData(childId, scheduleId, 1, "07:35", "08:15")
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route1)
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        val route2 = createRouteRequest(
            routeName = "Route 2",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicle2Id,
            startTime = "07:45",
            endTime = "09:15",
            children = listOf(
                childRequestData(childId, scheduleId, 1, "07:50", "08:30")
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route2)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("already"))
    }

    @Test
    fun `should allow same child on different routes on different days`() {
        val driverId = createDriver()
        val vehicle1Id = createVehicle()
        val vehicle2Id = createVehicle(registrationNumber = "WAW 5678B")
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val tomorrow = LocalDate.now().plusDays(1)
        val dayAfterTomorrow = LocalDate.now().plusDays(2)

        val route1 = createRouteRequest(
            routeName = "Route Day 1",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicle1Id,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route1)
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        val route2 = createRouteRequest(
            routeName = "Route Day 2",
            date = dayAfterTomorrow,
            driverId = driverId,
            vehicleId = vehicle2Id,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route2)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    fun `should reject route when driver already has route at same time`() {
        val driverId = createDriver()
        val vehicle1Id = createVehicle()
        val vehicle2Id = createVehicle(registrationNumber = "WAW 5678B")
        val child1Id = createChild(firstName = "Anna")
        val child2Id = createChild(firstName = "Jan")
        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route1 = createRouteRequest(
            routeName = "Route 1",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicle1Id,
            startTime = "07:30",
            endTime = "09:00",
            children = listOf(childRequestData(child1Id, schedule1Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route1)
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        val route2 = createRouteRequest(
            routeName = "Route 2",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicle2Id,
            startTime = "08:00",
            endTime = "09:30",
            children = listOf(childRequestData(child2Id, schedule2Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route2)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("driver"))
    }

    @Test
    fun `should allow same driver on different routes at different times same day`() {
        val driverId = createDriver()
        val vehicle1Id = createVehicle()
        val vehicle2Id = createVehicle(registrationNumber = "WAW 5678B")
        val child1Id = createChild(firstName = "Anna")
        val child2Id = createChild(firstName = "Jan")
        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route1 = createRouteRequest(
            routeName = "Morning Route",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicle1Id,
            startTime = "07:00",
            endTime = "09:00",
            children = listOf(childRequestData(child1Id, schedule1Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route1)
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        val route2 = createRouteRequest(
            routeName = "Afternoon Route",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicle2Id,
            startTime = "14:00",
            endTime = "16:00",
            children = listOf(childRequestData(child2Id, schedule2Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route2)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    fun `should reject route when vehicle already assigned to another route at same time`() {
        val driver1Id = createDriver(email = "driver1@test.com")
        val driver2Id = createDriver(email = "driver2@test.com")
        val vehicleId = createVehicle()
        val child1Id = createChild(firstName = "Anna")
        val child2Id = createChild(firstName = "Jan")
        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route1 = createRouteRequest(
            routeName = "Route 1",
            date = tomorrow,
            driverId = driver1Id,
            vehicleId = vehicleId,
            startTime = "07:30",
            endTime = "09:00",
            children = listOf(childRequestData(child1Id, schedule1Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route1)
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        val route2 = createRouteRequest(
            routeName = "Route 2",
            date = tomorrow,
            driverId = driver2Id,
            vehicleId = vehicleId,
            startTime = "08:00",
            endTime = "09:30",
            children = listOf(childRequestData(child2Id, schedule2Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route2)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("vehicle"))
    }

    @Test
    fun `should allow same vehicle on different routes at different times same day`() {
        val driver1Id = createDriver(email = "driver1@test.com")
        val driver2Id = createDriver(email = "driver2@test.com")
        val vehicleId = createVehicle()
        val child1Id = createChild(firstName = "Anna")
        val child2Id = createChild(firstName = "Jan")
        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route1 = createRouteRequest(
            routeName = "Morning Route",
            date = tomorrow,
            driverId = driver1Id,
            vehicleId = vehicleId,
            startTime = "07:00",
            endTime = "09:00",
            children = listOf(childRequestData(child1Id, schedule1Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route1)
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        val route2 = createRouteRequest(
            routeName = "Afternoon Route",
            date = tomorrow,
            driverId = driver2Id,
            vehicleId = vehicleId,
            startTime = "14:00",
            endTime = "16:00",
            children = listOf(childRequestData(child2Id, schedule2Id, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route2)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }
}

class RouteVehicleCapacityTests : BaseE2ETest() {

    @Test
    fun `should reject route when wheelchair requirement exceeds vehicle capacity`() {
        val driverId = createDriver()
        val vehicleId = createVehicle(
            totalSeats = 10,
            wheelchairSpaces = 1,
            childSeats = 8
        )

        val child1Id = createChild(
            firstName = "Anna",
            disability = listOf("PHYSICAL"),
        )
        val child2Id = createChild(
            firstName = "Jan",
            disability = listOf("PHYSICAL"),
        )

        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route = createRouteRequest(
            routeName = "Test Route",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(
                childRequestData(child1Id, schedule1Id, 1),
                childRequestData(child2Id, schedule2Id, 2)
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("wheelchair"))
    }

    @Test
    fun `should accept route when wheelchair requirement matches vehicle capacity`() {
        val driverId = createDriver()
        val vehicleId = createVehicle(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 8
        )

        val child1Id = createChild(
            firstName = "Anna",
            disability = listOf("PHYSICAL"),
        )
        val child2Id = createChild(
            firstName = "Jan",
            disability = listOf("PHYSICAL"),
        )

        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route = createRouteRequest(
            routeName = "Test Route",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(
                childRequestData(child1Id, schedule1Id, 1),
                childRequestData(child2Id, schedule2Id, 2)
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    fun `should reject route when special seat requirement exceeds vehicle capacity`() {
        val driverId = createDriver()
        val vehicleId = createVehicle(
            totalSeats = 10,
            wheelchairSpaces = 0,
            childSeats = 2
        )

        val child1Id = createChild(
            firstName = "Anna",
            disability = listOf("INTELLECTUAL"),
        )
        val child2Id = createChild(
            firstName = "Jan",
            disability = listOf("INTELLECTUAL"),
        )
        val child3Id = createChild(
            firstName = "Maria",
            disability = listOf("INTELLECTUAL"),
        )

        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)
        val schedule3Id = createSchedule(child3Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route = createRouteRequest(
            routeName = "Test Route",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(
                childRequestData(child1Id, schedule1Id, 1),
                childRequestData(child2Id, schedule2Id, 2),
                childRequestData(child3Id, schedule3Id, 3)
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("seat"))
    }

    @Test
    fun `should reject route when total children exceed vehicle total seats`() {
        val driverId = createDriver()
        val vehicleId = createVehicle(totalSeats = 2, wheelchairSpaces = 0, childSeats = 2)

        val child1Id = createChild(firstName = "Anna")
        val child2Id = createChild(firstName = "Jan")
        val child3Id = createChild(firstName = "Maria")

        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)
        val schedule3Id = createSchedule(child3Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route = createRouteRequest(
            routeName = "Test Route",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(
                childRequestData(child1Id, schedule1Id, 1),
                childRequestData(child2Id, schedule2Id, 2),
                childRequestData(child3Id, schedule3Id, 3)
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("capacity"))
    }

    @Test
    fun `should handle mixed requirements correctly`() {
        val driverId = createDriver()
        val vehicleId = createVehicle(
            totalSeats = 10,
            wheelchairSpaces = 2,
            childSeats = 5
        )

        val child1Id = createChild(
            firstName = "Anna",
            disability = listOf("PHYSICAL"),
        )
        val child2Id = createChild(
            firstName = "Jan",
            disability = listOf("INTELLECTUAL"),
        )
        val child3Id = createChild(
            firstName = "Maria",
            disability = listOf("AUTISM"),
        )
        val child4Id = createChild(
            firstName = "Piotr",
            disability = listOf("SENSORY_VISUAL")
        )

        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)
        val schedule3Id = createSchedule(child3Id)
        val schedule4Id = createSchedule(child4Id)

        val tomorrow = LocalDate.now().plusDays(1)

        val route = createRouteRequest(
            routeName = "Mixed Requirements Route",
            date = tomorrow,
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(
                childRequestData(child1Id, schedule1Id, 1),
                childRequestData(child2Id, schedule2Id, 2),
                childRequestData(child3Id, schedule3Id, 3),
                childRequestData(child4Id, schedule4Id, 4)
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }
}

class RouteStatusValidationTests : BaseE2ETest() {

    @Test
    fun `should reject route when driver is INACTIVE`() {
        val driverId = createDriver()

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverUpdateRequest(status = "INACTIVE"))
            .put("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.OK.value())

        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("driver"))
    }

    @Test
    fun `should reject route when driver is ON_LEAVE`() {
        val driverId = createDriver()

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverUpdateRequest(status = "ON_LEAVE"))
            .put("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.OK.value())

        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("driver"))
    }

    @Test
    fun `should reject route when vehicle is not AVAILABLE`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleUpdateRequest(status = "MAINTENANCE"))
            .put("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.OK.value())

        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("vehicle"))
    }

    @Test
    fun `should reject route with INACTIVE child`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val updateRequest = mapOf(
            "firstName" to "Anna",
            "lastName" to "Kowalska",
            "birthDate" to "2015-03-15",
            "status" to "INACTIVE",
            "disability" to listOf("INTELLECTUAL"),
            "transportNeeds" to mapOf(
                "wheelchair" to false,
                "specialSeat" to true,
                "safetyBelt" to true
            ),
            "notes" to "Test"
        )

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .put("/children/$childId")
            .then()
            .statusCode(HttpStatus.OK.value())

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("child"))
    }
}

class RouteTimeValidationTests : BaseE2ETest() {

    @Test
    fun `should reject route with date in the past`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().minusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("future"))
    }

    @Test
    fun `should accept route with today's date`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now(),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    fun `should reject route when estimatedEndTime is before estimatedStartTime`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            startTime = "09:00",
            endTime = "07:00",
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("after"))
    }

    @Test
    fun `should reject when child dropoff time is before pickup time`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            startTime = "07:00",
            endTime = "09:00",
            children = listOf(
                childRequestData(
                    childId = childId,
                    scheduleId = scheduleId,
                    pickupOrder = 1,
                    pickupTime = "08:00",
                    dropoffTime = "07:30"
                )
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("after"))
    }

    @Test
    fun `should validate pickup order is sequential and positive`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val child1Id = createChild(firstName = "Anna")
        val child2Id = createChild(firstName = "Jan")
        val schedule1Id = createSchedule(child1Id)
        val schedule2Id = createSchedule(child2Id)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(
                childRequestData(child1Id, schedule1Id, 0),
                childRequestData(child2Id, schedule2Id, 2)
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }
}

class RouteStatusTransitionTests : BaseE2ETest() {

    @Test
    fun `should not allow changing route back to PLANNED status`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to Instant.now().toString()))
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.OK.value())

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "PLANNED"))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("PLANNED"))
    }

    @Test
    fun `should require actualStartTime when starting route`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_PROGRESS"))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("start time"))
    }

    @Test
    fun `should require actualEndTime when completing route`() {
        val routeId = createTestRoute()

        val startTime = Instant.now()
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to startTime.toString()))
            .patch("/routes/$routeId/status")

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "COMPLETED"))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("end time"))
    }

    @Test
    fun `should not allow completing route before starting it`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "COMPLETED", "actualEndTime" to Instant.now().toString()))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("IN_PROGRESS"))
    }

    @Test
    fun `should allow cancelling PLANNED route`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "CANCELLED"))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo("CANCELLED"))
    }

    @Test
    fun `should allow cancelling IN_PROGRESS route`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to Instant.now().toString()))
            .patch("/routes/$routeId/status")

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "CANCELLED"))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo("CANCELLED"))
    }

    @Test
    fun `should not allow changing status of COMPLETED route`() {
        val routeId = createTestRoute()

        val startTime = Instant.now()
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to startTime.toString()))
            .patch("/routes/$routeId/status")

        val endTime = Instant.now().plusSeconds(3600)
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "COMPLETED", "actualEndTime" to endTime.toString()))
            .patch("/routes/$routeId/status")

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "CANCELLED"))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }
}

class RouteChildStatusTransitionTests : BaseE2ETest() {

    @Test
    fun `should not allow changing child status back to PENDING`() {
        val (routeId, childId) = createTestRouteWithChild()

        val pickupTime = Instant.now()
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_VEHICLE", "actualPickupTime" to pickupTime.toString()))
            .patch("/routes/$routeId/children/$childId/status")

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "PENDING"))
            .`when`()
            .patch("/routes/$routeId/children/$childId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("PENDING"))
    }

    @Test
    fun `should require actualPickupTime when picking up child`() {
        val (routeId, childId) = createTestRouteWithChild()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_VEHICLE"))
            .`when`()
            .patch("/routes/$routeId/children/$childId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("pickup time"))
    }

    @Test
    fun `should require actualDropoffTime when delivering child`() {
        val (routeId, childId) = createTestRouteWithChild()

        val pickupTime = Instant.now()
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_VEHICLE", "actualPickupTime" to pickupTime.toString()))
            .patch("/routes/$routeId/children/$childId/status")

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "DELIVERED"))
            .`when`()
            .patch("/routes/$routeId/children/$childId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("dropoff time"))
    }

    @Test
    fun `should not allow delivering child before picking up`() {
        val (routeId, childId) = createTestRouteWithChild()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "DELIVERED", "actualDropoffTime" to Instant.now().toString()))
            .`when`()
            .patch("/routes/$routeId/children/$childId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("IN_VEHICLE"))
    }

    @Test
    fun `should only allow marking PENDING child as ABSENT`() {
        val (routeId, childId) = createTestRouteWithChild()

        val pickupTime = Instant.now()
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_VEHICLE", "actualPickupTime" to pickupTime.toString()))
            .patch("/routes/$routeId/children/$childId/status")

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "ABSENT"))
            .`when`()
            .patch("/routes/$routeId/children/$childId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("PENDING"))
    }

    @Test
    fun `should validate dropoff time is after pickup time`() {
        val (routeId, childId) = createTestRouteWithChild()

        val pickupTime = Instant.now()
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_VEHICLE", "actualPickupTime" to pickupTime.toString()))
            .patch("/routes/$routeId/children/$childId/status")

        val dropoffTime = pickupTime.minusSeconds(60)
        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "DELIVERED", "actualDropoffTime" to dropoffTime.toString()))
            .`when`()
            .patch("/routes/$routeId/children/$childId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("after"))
    }
}

class RouteAuthorizationTests : BaseE2ETest() {

    @Test
    fun `DRIVER cannot create routes`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("DRIVER")
            .header("X-Test-Driver-Id", driverId)
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun `GUARDIAN cannot create routes`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        createAuthenticatedRequest("GUARDIAN")
            .body(route)
            .`when`()
            .post("/routes")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun `DRIVER can only update status of their own routes`() {
        val driver1Id = createDriver(email = "driver1@test.com")
        val driver2Id = createDriver(email = "driver2@test.com")
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driver1Id,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        val routeId = createAuthenticatedRequest("ADMIN")
            .body(route)
            .post("/routes")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("DRIVER")
            .header("X-Test-Driver-Id", driver2Id)
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to Instant.now().toString()))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("own"))
    }

    @Test
    fun `DRIVER can update status of their own route`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        val routeId = createAuthenticatedRequest("ADMIN")
            .body(route)
            .post("/routes")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("DRIVER")
            .header("X-Test-Driver-Id", driverId)
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to Instant.now().toString()))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.OK.value())
    }

    @Test
    fun `DRIVER can only add notes to their own routes`() {
        val driver1Id = createDriver(email = "driver1@test.com")
        val driver2Id = createDriver(email = "driver2@test.com")
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driver1Id,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        val routeId = createAuthenticatedRequest("ADMIN")
            .body(route)
            .post("/routes")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("DRIVER")
            .header("X-Test-Driver-Id", driver2Id)
            .body(mapOf("content" to "Test note"))
            .`when`()
            .post("/routes/$routeId/notes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("own"))
    }

    @Test
    fun `GUARDIAN cannot update route or child status`() {
        val driverId = createDriver()
        val vehicleId = createVehicle()
        val childId = createChild()
        val scheduleId = createSchedule(childId)

        val route = createRouteRequest(
            date = LocalDate.now().plusDays(1),
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        )

        val routeId = createAuthenticatedRequest("ADMIN")
            .body(route)
            .post("/routes")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("GUARDIAN")
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to Instant.now().toString()))
            .`when`()
            .patch("/routes/$routeId/status")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())

        createAuthenticatedRequest("GUARDIAN")
            .body(mapOf("status" to "IN_VEHICLE", "actualPickupTime" to Instant.now().toString()))
            .`when`()
            .patch("/routes/$routeId/children/$childId/status")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class RouteNoteValidationTests : BaseE2ETest() {

    @Test
    fun `should reject note exceeding 5000 characters`() {
        val routeId = createTestRoute()

        val longNote = "a".repeat(5001)

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("content" to longNote))
            .`when`()
            .post("/routes/$routeId/notes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `should reject empty note`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("content" to ""))
            .`when`()
            .post("/routes/$routeId/notes")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `should accept note with exactly 5000 characters`() {
        val routeId = createTestRoute()

        val validNote = "a".repeat(5000)

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("content" to validNote))
            .`when`()
            .post("/routes/$routeId/notes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    fun `should trim note content`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("content" to "  Test note with spaces  "))
            .`when`()
            .post("/routes/$routeId/notes")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("content", equalTo("Test note with spaces"))
    }
}

class RouteDeletionTests : BaseE2ETest() {

    @Test
    fun `should not delete route that is IN_PROGRESS`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "IN_PROGRESS", "actualStartTime" to Instant.now().toString()))
            .patch("/routes/$routeId/status")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/routes/$routeId")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsStringIgnoringCase("progress"))
    }

    @Test
    fun `should allow deleting PLANNED route`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/routes/$routeId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())
    }

    @Test
    fun `should allow deleting CANCELLED route`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("status" to "CANCELLED"))
            .patch("/routes/$routeId/status")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/routes/$routeId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())
    }

    @Test
    fun `should cascade delete route children and notes when deleting route`() {
        val routeId = createTestRoute()

        createAuthenticatedRequest("OPERATOR")
            .body(mapOf("content" to "Test note"))
            .post("/routes/$routeId/notes")

        createAuthenticatedRequest("ADMIN")
            .delete("/routes/$routeId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/routes/$routeId")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }
}

private fun BaseE2ETest.createDriver(email: String = "driver@test.com"): String {
    return createAuthenticatedRequest("ADMIN")
        .body(TestDataFactory.driverRequest(email = email))
        .post("/drivers")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .path("id")
}

private fun BaseE2ETest.createVehicle(
    registrationNumber: String = "WAW 1234A",
    totalSeats: Int = 12,
    wheelchairSpaces: Int = 2,
    childSeats: Int = 10
): String {
    return createAuthenticatedRequest("ADMIN")
        .body(TestDataFactory.vehicleRequest(
            registrationNumber = registrationNumber,
            totalSeats = totalSeats,
            wheelchairSpaces = wheelchairSpaces,
            childSeats = childSeats
        ))
        .post("/vehicles")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .path("id")
}

private fun BaseE2ETest.createChild(
    firstName: String = "Anna",
    disability: List<String> = listOf("INTELLECTUAL", "PHYSICAL")
): String {
    return createAuthenticatedRequest("ADMIN")
        .body(TestDataFactory.childRequest(
            firstName = firstName,
            disability = disability
        ))
        .post("/children")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .path("id")
}

private fun BaseE2ETest.createSchedule(childId: String): String {
    return createAuthenticatedRequest("ADMIN")
        .body(TestDataFactory.scheduleRequest())
        .post("/children/$childId/schedules")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .path("id")
}

private fun createRouteRequest(
    routeName: String = "Test Route",
    date: LocalDate = LocalDate.now().plusDays(1),
    driverId: String,
    vehicleId: String,
    startTime: String = "07:30",
    endTime: String = "09:00",
    children: List<Map<String, Any>>
): Map<String, Any> {
    return mapOf(
        "routeName" to routeName,
        "date" to date.toString(),
        "driverId" to driverId,
        "vehicleId" to vehicleId,
        "estimatedStartTime" to startTime,
        "estimatedEndTime" to endTime,
        "children" to children
    )
}

private fun childRequestData(
    childId: String,
    scheduleId: String,
    pickupOrder: Int,
    pickupTime: String = "07:35",
    dropoffTime: String = "08:15"
): Map<String, Any> {
    return mapOf(
        "childId" to childId,
        "scheduleId" to scheduleId,
        "pickupOrder" to pickupOrder,
        "pickupAddress" to mapOf(
            "street" to "ul. Testowa",
            "houseNumber" to pickupOrder.toString(),
            "postalCode" to "00-001",
            "city" to "Warszawa"
        ),
        "dropoffAddress" to mapOf(
            "street" to "ul. Szkolna",
            "houseNumber" to "1",
            "postalCode" to "00-002",
            "city" to "Warszawa"
        ),
        "estimatedPickupTime" to pickupTime,
        "estimatedDropoffTime" to dropoffTime
    )
}

private fun BaseE2ETest.createTestRoute(): String {
    val driverId = createDriver()
    val vehicleId = createVehicle()
    val childId = createChild()
    val scheduleId = createSchedule(childId)

    return createAuthenticatedRequest("ADMIN")
        .body(createRouteRequest(
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        ))
        .post("/routes")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .path("id")
}

private fun BaseE2ETest.createTestRouteWithChild(): Pair<String, String> {
    val driverId = createDriver()
    val vehicleId = createVehicle()
    val childId = createChild()
    val scheduleId = createSchedule(childId)

    val routeId = createAuthenticatedRequest("ADMIN")
        .body(createRouteRequest(
            driverId = driverId,
            vehicleId = vehicleId,
            children = listOf(childRequestData(childId, scheduleId, 1))
        ))
        .post("/routes")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .path<String>("id")

    return Pair(routeId, childId)
}