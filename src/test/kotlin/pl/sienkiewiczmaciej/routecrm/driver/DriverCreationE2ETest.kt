package pl.sienkiewiczmaciej.routecrm.driver

import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import pl.sienkiewiczmaciej.routecrm.test.BaseE2ETest
import pl.sienkiewiczmaciej.routecrm.test.TestDataFactory
import java.time.LocalDate

class DriverCreationE2ETest : BaseE2ETest() {

    @Test
    fun `should create driver successfully as ADMIN`() {
        val request = TestDataFactory.driverRequest()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("firstName", equalTo("Jan"))
            .body("lastName", equalTo("Nowak"))
            .body("status", equalTo("ACTIVE"))
            .body("drivingLicense.categories", hasItems("B", "D"))
    }

    @Test
    fun `should reject driver creation as OPERATOR`() {
        val request = TestDataFactory.driverRequest()

        createAuthenticatedRequest("OPERATOR")
            .body(request)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun `should validate driver must be at least 21 years old`() {
        val request = TestDataFactory.driverRequest(
            dateOfBirth = LocalDate.now().minusYears(20).toString()
        )

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("21"))
    }

    @Test
    fun `should require category D for bus drivers`() {
        val request = TestDataFactory.driverRequest().toMutableMap()
        (request["drivingLicense"] as MutableMap<String, Any?>)["categories"] = listOf("B", "C")

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("D"))
    }

    @Test
    fun `should validate license must be valid`() {
        val request = TestDataFactory.driverRequest().toMutableMap()
        (request["drivingLicense"] as MutableMap<String, Any?>)["validUntil"] =
            LocalDate.now().minusDays(1).toString()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `should validate medical certificate must be valid`() {
        val request = TestDataFactory.driverRequest().toMutableMap()
        (request["medicalCertificate"] as MutableMap<String, Any?>)["validUntil"] =
            LocalDate.now().minusDays(1).toString()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `should prevent duplicate email`() {
        val request = TestDataFactory.driverRequest(email = "duplicate@example.com")

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("already exists"))
    }

    @Test
    fun `should prevent duplicate license number`() {
        val licenseNumber = "ABC123456"
        val request1 = TestDataFactory.driverRequest(email = "driver1@example.com").toMutableMap()
        (request1["drivingLicense"] as MutableMap<String, Any?>)["licenseNumber"] = licenseNumber

        createAuthenticatedRequest("ADMIN")
            .body(request1)
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        val request2 = TestDataFactory.driverRequest(email = "driver2@example.com").toMutableMap()
        (request2["drivingLicense"] as MutableMap<String, Any?>)["licenseNumber"] = licenseNumber

        createAuthenticatedRequest("ADMIN")
            .body(request2)
            .`when`()
            .post("/drivers")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("license number"))
    }
}

class DriverQueryE2ETest : BaseE2ETest() {

    @Test
    fun `should list drivers with pagination`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest(firstName = "Jan"))
            .post("/drivers")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest(firstName = "Anna"))
            .post("/drivers")

        createAuthenticatedRequest("ADMIN")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/drivers")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", equalTo(2))
            .body("totalElements", equalTo(2))
    }

    @Test
    fun `should filter drivers by status`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")

        createAuthenticatedRequest("ADMIN")
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/drivers")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content[0].status", equalTo("ACTIVE"))
    }

    @Test
    fun `should search drivers by name or license number`() {
        val request = TestDataFactory.driverRequest(lastName = "Kowalski").toMutableMap()
        (request["drivingLicense"] as MutableMap<String, Any?>)["licenseNumber"] = "ABC999"

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .post("/drivers")

        createAuthenticatedRequest("ADMIN")
            .queryParam("search", "Kowalski")
            .`when`()
            .get("/drivers")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", greaterThan(0))

        createAuthenticatedRequest("ADMIN")
            .queryParam("search", "ABC999")
            .`when`()
            .get("/drivers")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", greaterThan(0))
    }

    @Test
    fun `should get driver by id`() {
        val driverId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(driverId))
            .body("drivingLicense.licenseNumber", notNullValue())
            .body("medicalCertificate.validUntil", notNullValue())
    }

    @Test
    fun `OPERATOR can view drivers`() {
        val driverId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("OPERATOR")
            .`when`()
            .get("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.OK.value())
    }
}

class DriverUpdateE2ETest : BaseE2ETest() {

    @Test
    fun `should update driver information`() {
        val driverId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        val updateRequest = TestDataFactory.driverUpdateRequest(
            firstName = "Updated",
            lastName = "Name",
            status = "ON_LEAVE"
        )

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("firstName", equalTo("Updated"))
            .body("status", equalTo("ON_LEAVE"))
    }

    @Test
    fun `should prevent email duplicate on update`() {
        val driver1Id = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest(email = "first@example.com"))
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest(email = "second@example.com"))
            .post("/drivers")

        val updateRequest = TestDataFactory.driverUpdateRequest(email = "second@example.com")

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/drivers/$driver1Id")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("already exists"))
    }

    @Test
    fun `should prevent license number duplicate on update`() {
        val driver1Id = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest(email = "driver1@example.com"))
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        val licenseNumber = "XYZ999"
        val request2 = TestDataFactory.driverRequest(email = "driver2@example.com").toMutableMap()
        (request2["drivingLicense"] as MutableMap<String, Any?>)["licenseNumber"] = licenseNumber

        createAuthenticatedRequest("ADMIN")
            .body(request2)
            .post("/drivers")

        val updateRequest = TestDataFactory.driverUpdateRequest(email = "driver1@example.com").toMutableMap()
        (updateRequest["drivingLicense"] as MutableMap<String, Any?>)["licenseNumber"] = licenseNumber

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/drivers/$driver1Id")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("license number"))
    }

    @Test
    fun `only ADMIN can update drivers`() {
        val driverId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        val updateRequest = TestDataFactory.driverUpdateRequest(firstName = "Updated")

        createAuthenticatedRequest("OPERATOR")
            .body(updateRequest)
            .`when`()
            .put("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class DriverDeleteE2ETest : BaseE2ETest() {

    @Test
    fun `should soft delete driver`() {
        val driverId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo("INACTIVE"))
    }

    @Test
    fun `only ADMIN can delete drivers`() {
        val driverId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("OPERATOR")
            .`when`()
            .delete("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class DriverMultiTenancyE2ETest : BaseE2ETest() {

    @Test
    fun `should enforce multi-tenancy isolation`() {
        val company1 = createTestCompany()
        val user1 = createTestUser(company1.id, "ADMIN")

        val driverId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.driverRequest())
            .post("/drivers")
            .then()
            .extract()
            .path<String>("id")

        val company2 = createTestCompany("CMP-test-2")
        val user2 = createTestUser(company2.id, "ADMIN")

        io.restassured.RestAssured.given()
            .contentType(io.restassured.http.ContentType.JSON)
            .header("X-Test-User-Id", user2.id)
            .header("X-Test-Company-Id", company2.id)
            .header("X-Test-User-Role", "ADMIN")
            .`when`()
            .get("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())

        io.restassured.RestAssured.given()
            .contentType(io.restassured.http.ContentType.JSON)
            .header("X-Test-User-Id", user1.id)
            .header("X-Test-Company-Id", company1.id)
            .header("X-Test-User-Role", "ADMIN")
            .`when`()
            .get("/drivers/$driverId")
            .then()
            .statusCode(HttpStatus.OK.value())
    }
}