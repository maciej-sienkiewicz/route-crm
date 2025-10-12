package pl.sienkiewiczmaciej.routecrm.vehicle

import io.restassured.RestAssured.given
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import pl.sienkiewiczmaciej.routecrm.test.BaseE2ETest
import pl.sienkiewiczmaciej.routecrm.test.TestDataFactory
import java.time.LocalDate

class VehicleCreationE2ETest : BaseE2ETest() {

    @Test
    fun `should create vehicle`() {
        val request = TestDataFactory.vehicleRequest()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("registrationNumber", equalTo("WAW 1234A"))
            .body("make", equalTo("Mercedes"))
            .body("model", equalTo("Sprinter"))
            .body("year", equalTo(2022))
            .body("vehicleType", equalTo("MICROBUS"))
            .body("status", equalTo("AVAILABLE"))
            .body("capacity.totalSeats", equalTo(12))
            .body("capacity.wheelchairSpaces", equalTo(2))
            .body("currentMileage", equalTo(0))
    }

    @Test
    fun `should validate registration number format`() {
        val request = TestDataFactory.vehicleRequest(registrationNumber = "INVALID")

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("errors.registrationNumber", containsString("registration number"))
    }

    @Test
    fun `should validate year range`() {
        val request = TestDataFactory.vehicleRequest(year = 1980)

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("errors.year", containsString("1990"))
    }

    @Test
    fun `should create vehicle without insurance and technical inspection`() {
        val request = TestDataFactory.vehicleRequestWithoutDocuments()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("id", notNullValue())
            .body("registrationNumber", equalTo("WAW 5555E"))
            .body("insurance", nullValue())
            .body("technicalInspection", nullValue())
    }

    @Test
    fun `should validate insurance is in future`() {
        val request = TestDataFactory.vehicleRequest(
            insuranceValidUntil = LocalDate.now().minusDays(1).toString()
        )

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    @Test
    fun `should prevent duplicate registration numbers`() {
        val request = TestDataFactory.vehicleRequest()

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("already exists"))
    }

    @Test
    fun `should reject vehicle creation as OPERATOR role`() {
        val request = TestDataFactory.vehicleRequest()

        createAuthenticatedRequest("OPERATOR")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class VehicleQueryE2ETest : BaseE2ETest() {

    @Test
    fun `should list vehicles with pagination and filtering`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest(registrationNumber = "WAW 1111A"))
            .post("/vehicles")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest(registrationNumber = "WAW 2222B", vehicleType = "BUS"))
            .post("/vehicles")

        createAuthenticatedRequest("ADMIN")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .queryParam("status", "AVAILABLE")
            .`when`()
            .get("/vehicles")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", equalTo(2))
            .body("totalElements", equalTo(2))
    }

    @Test
    fun `should filter vehicles by type`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest(registrationNumber = "WAW 1111A", vehicleType = "MICROBUS"))
            .post("/vehicles")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest(registrationNumber = "WAW 2222B", vehicleType = "BUS"))
            .post("/vehicles")

        createAuthenticatedRequest("ADMIN")
            .queryParam("vehicleType", "MICROBUS")
            .`when`()
            .get("/vehicles")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content.size()", equalTo(1))
            .body("content[0].vehicleType", equalTo("MICROBUS"))
    }

    @Test
    fun `should get vehicle by id`() {
        val vehicleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest())
            .post("/vehicles")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(vehicleId))
            .body("registrationNumber", notNullValue())
            .body("specialEquipment", notNullValue())
            .body("insurance.policyNumber", notNullValue())
            .body("technicalInspection.inspectionStation", notNullValue())
    }

    @Test
    fun `OPERATOR can view vehicles`() {
        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest())
            .post("/vehicles")

        createAuthenticatedRequest("OPERATOR")
            .`when`()
            .get("/vehicles")
            .then()
            .statusCode(HttpStatus.OK.value())
    }

    @Test
    fun `GUARDIAN cannot view vehicles`() {
        createAuthenticatedRequest("GUARDIAN")
            .`when`()
            .get("/vehicles")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class VehicleUpdateE2ETest : BaseE2ETest() {

    @Test
    fun `should update vehicle information`() {
        val vehicleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest())
            .post("/vehicles")
            .then()
            .extract()
            .path<String>("id")

        val updateRequest = TestDataFactory.vehicleUpdateRequest(
            registrationNumber = "WAW 9999Z",
            status = "MAINTENANCE",
            currentMileage = 50000
        )

        createAuthenticatedRequest("ADMIN")
            .body(updateRequest)
            .`when`()
            .put("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("registrationNumber", equalTo("WAW 9999Z"))
            .body("status", equalTo("MAINTENANCE"))
            .body("currentMileage", equalTo(50000))

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/vehicles/$vehicleId")
            .then()
            .body("status", equalTo("MAINTENANCE"))
    }

    @Test
    fun `should not allow mileage to decrease`() {
        val vehicleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest())
            .post("/vehicles")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleUpdateRequest(currentMileage = 5000))
            .`when`()
            .put("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("cannot decrease"))
    }

    @Test
    fun `should prevent duplicate registration number on update`() {
        val vehicle1Id = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest(registrationNumber = "WAW 1111A"))
            .post("/vehicles")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest(registrationNumber = "WAW 2222B"))
            .post("/vehicles")

        createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleUpdateRequest(registrationNumber = "WAW 2222B"))
            .`when`()
            .put("/vehicles/$vehicle1Id")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("already exists"))
    }

    @Test
    fun `only ADMIN can update vehicles`() {
        val vehicleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest())
            .post("/vehicles")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("OPERATOR")
            .body(TestDataFactory.vehicleUpdateRequest())
            .`when`()
            .put("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class VehicleDeleteE2ETest : BaseE2ETest() {

    @Test
    fun `should delete vehicle`() {
        val vehicleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest())
            .post("/vehicles")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .delete("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        createAuthenticatedRequest("ADMIN")
            .`when`()
            .get("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `only ADMIN can delete vehicles`() {
        val vehicleId = createAuthenticatedRequest("ADMIN")
            .body(TestDataFactory.vehicleRequest())
            .post("/vehicles")
            .then()
            .extract()
            .path<String>("id")

        createAuthenticatedRequest("OPERATOR")
            .`when`()
            .delete("/vehicles/$vehicleId")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}

class VehicleCapacityValidationE2ETest : BaseE2ETest() {

    @Test
    fun `should validate total seats range`() {
        val request = TestDataFactory.vehicleRequest(totalSeats = 0)

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("Total seats"))
    }

    @Test
    fun `should validate wheelchair spaces do not exceed total seats`() {
        val request = TestDataFactory.vehicleRequest(
            totalSeats = 10,
            wheelchairSpaces = 15
        )

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", containsString("cannot exceed total seats"))
    }

    @Test
    fun `should allow valid capacity configuration`() {
        val request = TestDataFactory.vehicleRequest(
            totalSeats = 20,
            wheelchairSpaces = 3,
            childSeats = 17
        )

        createAuthenticatedRequest("ADMIN")
            .body(request)
            .`when`()
            .post("/vehicles")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("capacity.totalSeats", equalTo(20))
            .body("capacity.wheelchairSpaces", equalTo(3))
            .body("capacity.childSeats", equalTo(17))
    }
}