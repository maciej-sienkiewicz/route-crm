package pl.sienkiewiczmaciej.routecrm.shared.infrastructure.config

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyEntity
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.UserEntity
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.UserJpaRepository
import java.time.Instant

@Component
class DataInitializer(
    private val userRepository: UserJpaRepository,
    private val companyRepository: CompanyJpaRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) = runBlocking {
        initializeTestData()
    }

    private suspend fun initializeTestData() {
        // Sprawdź czy admin już istnieje
        if (userRepository.findByEmail("admin@admin.com") != null) {
            logger.info("Test admin user already exists, skipping initialization")
            return
        }

        logger.info("Initializing test data...")

        // Utwórz testową firmę
        val companyId = CompanyId.generate()
        val company = CompanyEntity(
            id = companyId.value,
            name = "Demo Transport Company",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        companyRepository.save(company)
        logger.info("Created test company: ${company.name} (${company.id})")

        // Utwórz admina: admin/admin
        val adminId = UserId.generate()
        val admin = UserEntity(
            id = adminId.value,
            companyId = companyId.value,
            email = "admin@admin.com",
            passwordHash = passwordEncoder.encode("admin"),
            firstName = "Admin",
            lastName = "Administrator",
            role = UserRole.ADMIN,
            guardianId = null,
            driverId = null,
            active = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        userRepository.save(admin)
        logger.info("Created admin user: admin@admin.com / admin")

        // Utwórz operatora: operator/operator
        val operatorId = UserId.generate()
        val operator = UserEntity(
            id = operatorId.value,
            companyId = companyId.value,
            email = "operator@demo.com",
            passwordHash = passwordEncoder.encode("operator"),
            firstName = "John",
            lastName = "Operator",
            role = UserRole.OPERATOR,
            guardianId = null,
            driverId = null,
            active = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        userRepository.save(operator)
        logger.info("Created operator user: operator@demo.com / operator")

        logger.info("""
            |
            |===========================================
            | Test users created successfully!
            |===========================================
            | Admin:
            |   Email: admin@admin.com
            |   Password: admin
            |   Role: ADMIN
            |
            | Operator:
            |   Email: operator@demo.com
            |   Password: operator
            |   Role: OPERATOR
            |
            | Company: ${company.name}
            | Company ID: ${company.id}
            |===========================================
            |
        """.trimMargin())
    }
}