package pl.sienkiewiczmaciej.routecrm.guardian

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import pl.sienkiewiczmaciej.routecrm.test.BaseE2ETest
import pl.sienkiewiczmaciej.routecrm.test.TestDataFactory

class GuardianCreationE2ETest : BaseE2ETest() {

    @Test
    fun `should create guardian successfully as ADMIN`() {
        val request = TestDataFactory.guardianRequest()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/guardians")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("firstName", equalTo("Jan"))
            .body("lastName", equalTo("Kowalski"))
            .body("email", startsWith("jan.kowalski"))
            .body("phone", equalTo("+48123456789"))
            .body("communicationPreference", equalTo("SMS"))
            .body("childrenCount", equalTo(0))
    }

    @Test
    fun `should create guardian successfully as OPERATOR`() {
        val request = TestDataFactory.guardianRequest()

        createAuthenticatedRequest("OPERATOR")
            .body(request)
            .`when`()
            .post("/guardians")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    fun `should reject guardian creation as GUARDIAN role`() {
        val request = TestDataFactory.guardianRequest()

        createAuthenticatedRequest("GUARDIAN")
            .body(request)
            .`when`()
            .post("/guardians")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun `should reject guardian creation with duplicate email`() {
        val request = TestDataFactory.guardianRequest(email = "duplicate@example.com")

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/guardians")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/guardians")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("already exists"))
    }

    @Test
    fun `should validate required fields`() {
        val invalidRequest = mapOf(
            "firstName" to "",
            "lastName" to "",
            "email" to "invalid-email",
            "phone" to "invalid",
            "address" to mapOf(
                "street" to "",
                "houseNumber" to "",
                "postalCode" to "invalid",
                "city" to ""
            )
        )

        createAuthenticatedRequest("ADMIN")
            .body(invalidRequest)
            .`when`()
            .post("/guardians")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `should validate postal code format`() {
        val request = TestDataFactory.guardianRequest().toMutableMap()
        (request["address"] as MutableMap<String, Any?>)["postalCode"] = "invalid"

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/guardians")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("postal code"))
    }
}

class GuardianQueryE2ETest : BaseE2ETest() {

    @Test
    fun `should list guardians with pagination`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest(firstName = "Jan"))
            .post("/guardians")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest(firstName = "Anna"))
            .post("/guardians")

        createAuthenticatedRequest("ADMIN")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/guardians")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", equalTo(2))
            .body("totalElements", equalTo(2))
            .body("totalPages", equalTo(1))
    }

    @Test
    fun `should search guardians by name`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest(firstName = "Jan", lastName = "Kowalski"))
            .post("/guardians")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest(firstName = "Anna", lastName = "Nowak"))
            .post("/guardians")

        createAuthenticatedRequest("ADMIN")
            .queryParam("search", "Kowalski")
            .`when`()
            .get("/guardians")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", equalTo(1))
            .body("content[0].lastName", equalTo("Kowalski"))
    }

    @Test
    fun `should get guardian by id`() {
        val guardianId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest())
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/guardians/$guardianId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(guardianId))
            .body("firstName", equalTo("Jan"))
            .body("address.street", equalTo("ul. Marszałkowska"))
    }

    @Test
    fun `should return 404 for non-existent guardian`() {
        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/guardians/GRD-nonexistent")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }
}

class GuardianUpdateE2ETest : BaseE2ETest() {

    @Test
    fun `should update guardian successfully`() {
        val guardianId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest())
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        val updateRequest = TestDataFactory.guardianRequest(
            firstName = "Updated",
            communicationPreference = "EMAIL"
        )

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/guardians/$guardianId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("firstName", equalTo("Updated"))

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/guardians/$guardianId")
            .then()
            .body("communicationPreference", equalTo("EMAIL"))
    }

    @Test
    fun `should prevent email duplicate on update`() {
        val guardian1Id = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest(email = "first@example.com"))
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.guardianRequest(email = "second@example.com"))
            .post("/guardians")

        val updateRequest = TestDataFactory.guardianRequest(email = "second@example.com")

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/guardians/$guardian1Id")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("already exists"))
    }
}

class GuardianAuthorizationE2ETest : BaseE2ETest() {

    @Test
    fun `should enforce multi-tenancy isolation`() {
        val company1 = createTestCompany()
        val user1 = createTestUser(company1.id, "ADMIN")

        val guardianId = given()
            .contentType(ContentType.JSON)
            .header("X-Test-User-Id", user1.id)
            .header("X-Test-Company-Id", company1.id)
            .header("X-Test-User-Role", "ADMIN")
            .body(TestDataFactory.guardianRequest())
            .post("/guardians")
            .then()
            .extract()
            .path<String>("id")

        val company2 = createTestCompany()
        val user2 = createTestUser(company2.id, "ADMIN")

        given()
            .contentType(ContentType.JSON)
            .header("X-Test-User-Id", user2.id)
            .header("X-Test-Company-Id", company2.id)
            .header("X-Test-User-Role", "ADMIN")
            .`when`()
            .get("/guardians/$guardianId")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }
}