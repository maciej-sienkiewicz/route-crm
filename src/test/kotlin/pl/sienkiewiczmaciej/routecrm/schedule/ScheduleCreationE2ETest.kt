package pl.sienkiewiczmaciej.routecrm.schedule

import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import pl.sienkiewiczmaciej.routecrm.test.BaseE2ETest
import pl.sienkiewiczmaciej.routecrm.test.TestDataFactory
import java.time.LocalDate

class ScheduleCreationE2ETest : BaseE2ETest() {

    @Test
    fun `should create schedule for child`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val request = TestDataFactory.scheduleRequest()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/children/$childId/schedules")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("name", equalTo("Do szkoły"))
            .body("days", hasItems("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
            .body("pickupTime", equalTo("07:30"))
            .body("dropoffTime", equalTo("08:15"))
            .body("active", equalTo(true))
    }

    @Test
    fun `should validate dropoff time after pickup time`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val request = TestDataFactory.scheduleRequest(
            pickupTime = "08:00",
            dropoffTime = "07:00"
        )

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/children/$childId/schedules")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("after"))
    }

    @Test
    fun `should require at least one day`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val request = TestDataFactory.scheduleRequest(days = emptyList())

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/children/$childId/schedules")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }
}

class ScheduleQueryE2ETest : BaseE2ETest() {

    @Test
    fun `should list schedules for child`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest(name = "Morning"))
            .post("/children/$childId/schedules")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest(name = "Afternoon"))
            .post("/children/$childId/schedules")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/children/$childId/schedules")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("schedules.size()", equalTo(2))
            .body("schedules[0].name", notNullValue())
    }

    @Test
    fun `GUARDIAN should access schedules of own child`() {
        val guardianId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest())
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest(guardianId = guardianId))
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")

        createAuthenticatedRequest("GUARDIAN")
            .`when`()
            .get("/children/$childId/schedules")
            .then()
            .statusCode(HttpStatus.OK.value())
    }
}

class ScheduleUpdateE2ETest : BaseE2ETest() {

    @Test
    fun `should update schedule`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        val updateRequest = TestDataFactory.scheduleRequest(
            name = "Updated Schedule",
            days = listOf("MONDAY", "WEDNESDAY", "FRIDAY")
        ).toMutableMap().apply {
            put("active", false)
        }

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/schedules/$scheduleId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("name", equalTo("Updated Schedule"))
    }

    @Test
    fun `should delete schedule`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/schedules/$scheduleId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/schedules/$scheduleId")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }
}

class ScheduleExceptionE2ETest : BaseE2ETest() {

    @Test
    fun `should create schedule exception`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        val exceptionRequest = TestDataFactory.scheduleExceptionRequest()

        createAuthenticatedRequest("ADMIN")
            .body(exceptionRequest)
            .`when`()
            .post("/schedules/$scheduleId/exceptions")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("scheduleId", equalTo(scheduleId))
            .body("childId", equalTo(childId))
            .body("exceptionDate", notNullValue())
            .body("createdByRole", equalTo("ADMIN"))
    }

    @Test
    fun `GUARDIAN can create exception for own child schedule`() {
        val guardianId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest())
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest(guardianId = guardianId))
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("GUARDIAN")
            .body(TestDataFactory.scheduleExceptionRequest())
            .`when`()
            .post("/schedules/$scheduleId/exceptions")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("createdByRole", equalTo("GUARDIAN"))
    }

    @Test
    fun `should prevent duplicate exception for same schedule and date`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        val exceptionDate = LocalDate.now().plusDays(7).toString()
        val exceptionRequest = TestDataFactory.scheduleExceptionRequest(exceptionDate = exceptionDate)

        createAuthenticatedRequest("ADMIN")
            .body(exceptionRequest)
            .post("/schedules/$scheduleId/exceptions")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        createAuthenticatedRequest("ADMIN")
            .body(exceptionRequest)
            .`when`()
            .post("/schedules/$scheduleId/exceptions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("already exists"))
    }

    @Test
    fun `should validate exception date cannot be in past`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        val pastDate = LocalDate.now().minusDays(1).toString()
        val exceptionRequest = TestDataFactory.scheduleExceptionRequest(exceptionDate = pastDate)

        createAuthenticatedRequest("ADMIN")
            .body(exceptionRequest)
            .`when`()
            .post("/schedules/$scheduleId/exceptions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("past"))
    }

    @Test
    fun `should list exceptions for schedule with date filtering`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleExceptionRequest(exceptionDate = LocalDate.now().plusDays(1).toString()))
            .post("/schedules/$scheduleId/exceptions")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleExceptionRequest(exceptionDate = LocalDate.now().plusDays(5).toString()))
            .post("/schedules/$scheduleId/exceptions")

        createAuthenticatedRequest("ADMIN")
            .queryParam("from", LocalDate.now().toString())
            .queryParam("to", LocalDate.now().plusDays(10).toString())
            .`when`()
            .get("/schedules/$scheduleId/exceptions")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("exceptions.size()", equalTo(2))
    }

    @Test
    fun `should get child exception statistics`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleExceptionRequest(exceptionDate = LocalDate.now().plusDays(1).toString()))
            .post("/schedules/$scheduleId/exceptions")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleExceptionRequest(exceptionDate = LocalDate.now().plusDays(2).toString()))
            .post("/schedules/$scheduleId/exceptions")

        createAuthenticatedRequest("ADMIN")
            .queryParam("year", LocalDate.now().year)
            .queryParam("month", LocalDate.now().monthValue)
            .`when`()
            .get("/children/$childId/exception-stats")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("childId", equalTo(childId))
            .body("totalExceptions", equalTo(2))
            .body("periodFrom", notNullValue())
            .body("periodTo", notNullValue())
    }

    @Test
    fun `should delete exception`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val scheduleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest())
            .post("/children/$childId/schedules")
            .then()
            .extract()
            .path<String>("id")

        val exceptionId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleExceptionRequest())
            .post("/schedules/$scheduleId/exceptions")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/exceptions/$exceptionId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())
    }
}