package pl.sienkiewiczmaciej.routecrm.test

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.jdbc.core.JdbcTemplate

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseE2ETest {

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("test_transport_crm")
            .withUsername("test")
            .withPassword("test")

        @Container
        val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // PostgreSQL
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)

            // Redis
            registry.add("spring.data.redis.host", redisContainer::getHost)
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379).toString() }

            // Disable session for tests (we're using test auth headers)
            registry.add("spring.session.store-type") { "none" }
        }
    }

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.basePath = "/api"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        cleanDatabase()
    }

    protected fun cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE schedule_exceptions CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE route_notes CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE route_children CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE routes CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE schedules CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE guardian_assignments CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE children CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE guardians CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE drivers CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE vehicles CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE companies CASCADE")
    }

    protected fun createAuthenticatedRequest(userRole: String = "ADMIN"): io.restassured.specification.RequestSpecification {
        val company = createTestCompany()
        val user = createTestUser(company.id, userRole)

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .header("X-Test-User-Id", user.id)
            .header("X-Test-Company-Id", company.id)
            .header("X-Test-User-Role", userRole)
    }

    protected data class TestCompany(val id: String, val name: String)
    protected data class TestUser(val id: String, val companyId: String, val email: String, val role: String)

    protected fun createTestCompany(): TestCompany {
        val id = "CMP-test-${System.currentTimeMillis()}"
        jdbcTemplate.update(
            "INSERT INTO companies (id, name, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
            id, "Test Company"
        )
        return TestCompany(id, "Test Company")
    }

    protected fun createTestUser(companyId: String, role: String): TestUser {
        val id = "USR-test-${System.currentTimeMillis()}"
        val email = "test-${System.currentTimeMillis()}@example.com"
        jdbcTemplate.update(
            """INSERT INTO users (id, company_id, email, password_hash, first_name, last_name, role, active, created_at, updated_at) 
               VALUES (?, ?, ?, 'hash', 'Test', 'User', ?, true, NOW(), NOW())""",
            id, companyId, email, role
        )
        return TestUser(id, companyId, email, role)
    }
}