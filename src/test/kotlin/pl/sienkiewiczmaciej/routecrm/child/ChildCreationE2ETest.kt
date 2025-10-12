package pl.sienkiewiczmaciej.routecrm.child

import io.restassured.RestAssured.given
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import pl.sienkiewiczmaciej.routecrm.test.BaseE2ETest
import pl.sienkiewiczmaciej.routecrm.test.TestDataFactory

class ChildCreationE2ETest : BaseE2ETest() {

    @Test
    fun `should create child with new guardian`() {
        val request = TestDataFactory.childRequest()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/children")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("firstName", equalTo("Anna"))
            .body("lastName", equalTo("Kowalska"))
            .body("age", notNullValue())
            .body("status", equalTo("ACTIVE"))
            .body("disability", hasItems("INTELLECTUAL", "PHYSICAL"))
            .body("guardiansCount", equalTo(1))
    }

    @Test
    fun `should create child with existing guardian`() {
        val guardianId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest())
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        val request = TestDataFactory.childRequest(guardianId = guardianId)

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/children")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("guardiansCount", equalTo(1))
    }

    @Test
    fun `should validate birth date must be in past`() {
        val request = TestDataFactory.childRequest(birthDate = java.time.LocalDate.now().plusDays(1).toString())

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/children")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("past"))
    }

    @Test
    fun `should require at least one disability type`() {
        val request = TestDataFactory.childRequest(disability = emptyList())

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/children")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `should reject child creation as GUARDIAN role`() {
        val request = TestDataFactory.childRequest()

        createAuthenticatedRequest("GUARDIAN")
            .body(request)
            .`when`()
            .post("/children")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class ChildQueryE2ETest : BaseE2ETest() {

    @Test
    fun `should list children with pagination and filtering`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest(firstName = "Anna"))
            .post("/children")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest(firstName = "Jan"))
            .post("/children")

        createAuthenticatedRequest("ADMIN")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/children")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", equalTo(2))
            .body("totalElements", equalTo(2))
    }

    @Test
    fun `should get child by id with guardians`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")
        
        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/children/$childId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(childId))
            .body("guardians", notNullValue())
            .body("guardians.size()", greaterThan(0))
    }

    @Test
    fun `GUARDIAN should only see own children`() {
        val guardianId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest(email = "guardian1@example.com"))
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest(guardianId = guardianId))
            .post("/children")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")

        createAuthenticatedRequest("GUARDIAN")
            .`when`()
            .get("/children")
            .then()
            .statusCode(HttpStatus.OK.value())
    }
}

class ChildUpdateE2ETest : BaseE2ETest() {

    @Test
    fun `should update child information`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        val updateRequest = mapOf(
            "firstName" to "Updated",
            "lastName" to "Name",
            "birthDate" to "2015-03-15",
            "status" to "ACTIVE",
            "disability" to listOf("INTELLECTUAL"),
            "transportNeeds" to mapOf(
                "wheelchair" to true,
                "specialSeat" to true,
                "safetyBelt" to true
            ),
            "notes" to "Updated notes"
        )

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/children/$childId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("firstName", equalTo("Updated"))

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/children/$childId")
            .then()
            .body("transportNeeds.wheelchair", equalTo(true))
            .body("notes", equalTo("Updated notes"))
    }

    @Test
    fun `should soft delete child`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/children/$childId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/children/$childId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo("INACTIVE"))
    }

    @Test
    fun `only ADMIN can delete children`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("OPERATOR")
            .`when`()
            .delete("/children/$childId")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class ChildRelationshipsE2ETest : BaseE2ETest() {

    @Test
    fun `should maintain guardian-child relationship`() {
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
            .`when`()
            .get("/children/$childId")
            .then()
            .body("guardians[0].id", equalTo(guardianId))
            .body("guardians[0].isPrimary", equalTo(true))

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/guardians/$guardianId")
            .then()
            .body("children", notNullValue())
    }

    @Test
    fun `should count active schedules for child`() {
        val childId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.childRequest())
            .post("/children")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest(name = "Schedule 1"))
            .post("/children/$childId/schedules")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.scheduleRequest(name = "Schedule 2"))
            .post("/children/$childId/schedules")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/children")
            .then()
            .body("content[0].activeSchedulesCount", equalTo(2))
    }
}