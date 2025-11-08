package pl.sienkiewiczmaciej.routecrm.shared.infrastructure.config

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.*
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildEntity
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.driver.domain.DrivingLicense
import pl.sienkiewiczmaciej.routecrm.driver.domain.MedicalCertificate
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverEntity
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.CommunicationPreference
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentEntity
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAssignmentJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianEntity
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianRelationship
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.AddressEmbeddable
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleEntity
import pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserId
import pl.sienkiewiczmaciej.routecrm.shared.domain.UserRole
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyEntity
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.UserEntity
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.UserJpaRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Insurance
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.TechnicalInspection
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.Vehicle
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleCapacity
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleType
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleEntity
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Component
class DataInitializer(
    private val userRepository: UserJpaRepository,
    private val companyRepository: CompanyJpaRepository,
    private val guardianRepository: GuardianJpaRepository,
    private val childRepository: ChildJpaRepository,
    private val guardianAssignmentRepository: GuardianAssignmentJpaRepository,
    private val scheduleRepository: ScheduleJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val vehicleRepository: VehicleJpaRepository,
    private val driverJpaRepository: DriverJpaRepository,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) = runBlocking {
        initializeTestData()
    }

    private suspend fun initializeTestData() {
        logger.info("Initializing test data...")

        // Utwórz testową firmę
        val companyId = CompanyId.generate()
        val companyBefore = CompanyEntity(
            id = companyId.value,
            name = "Demo Transport Company",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val company = companyRepository.save(companyBefore)
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

        // Utwórz opiekunów
        val guardians = createGuardians(companyId.value)
        logger.info("Created ${guardians.size} guardians")

        // Utwórz dzieci
        val children = createChildren(companyId.value)
        logger.info("Created ${children.size} children")

        // Przypisz opiekunów do dzieci
        assignGuardiansToChildren(companyId.value, guardians, children)
        logger.info("Created guardian assignments")

        createDrivers(companyId.value)
        createVehicles(companyId.value)

        // Utwórz harmonogramy
        createSchedules(companyId.value, children)
        logger.info("Created schedules")

        logger.info("""
            |
            |===========================================
            | Test data created successfully!
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
            |
            | Guardians: ${guardians.size}
            | Children: ${children.size}
            | Schedules: ~${children.size * 3}
            |===========================================
            |
        """.trimMargin())
    }

    private fun createGuardians(companyId: String): List<GuardianEntity> {
        val guardianData = listOf(
            Triple("Anna", "Kowalska", "anna.kowalska@example.com"),
            Triple("Jan", "Nowak", "jan.nowak@example.com"),
            Triple("Maria", "Wiśniewska", "maria.wisniewska@example.com"),
            Triple("Piotr", "Wójcik", "piotr.wojcik@example.com"),
            Triple("Katarzyna", "Kamińska", "katarzyna.kaminska@example.com"),
            Triple("Tomasz", "Lewandowski", "tomasz.lewandowski@example.com"),
            Triple("Magdalena", "Zielińska", "magdalena.zielinska@example.com"),
            Triple("Krzysztof", "Szymański", "krzysztof.szymanski@example.com"),
            Triple("Agnieszka", "Dąbrowska", "agnieszka.dabrowska@example.com"),
            Triple("Marcin", "Kozłowski", "marcin.kozlowski@example.com"),
            Triple("Joanna", "Jankowska", "joanna.jankowska@example.com"),
            Triple("Paweł", "Mazur", "pawel.mazur@example.com"),
            Triple("Monika", "Krawczyk", "monika.krawczyk@example.com"),
            Triple("Robert", "Piotrowski", "robert.piotrowski@example.com"),
            Triple("Ewa", "Grabowska", "ewa.grabowska@example.com")
        )

        val streets = listOf(
            "ul. Słowackiego", "ul. Mickiewicza", "ul. Kościuszki", "ul. Piłsudskiego",
            "ul. Sienkiewicza", "ul. Żeromskiego", "ul. Prusa", "ul. Reymonta",
            "ul. Konopnickiej", "ul. Orzeszkowej", "ul. Kraszewskiego", "ul. Wyspiańskiego",
            "ul. Norwida", "ul. Fredry", "ul. Staffa"
        )

        val cities = listOf(
            "Poznań", "Warszawa", "Kraków", "Wrocław", "Gdańsk"
        )

        return guardianData.mapIndexed { index, (firstName, lastName, email) ->
            val guardian = GuardianEntity(
                id = "G-${UUID.randomUUID()}",
                companyId = companyId,
                firstName = firstName,
                lastName = lastName,
                email = "$index$email",
                phone = "+48${(500000000 + index * 111111).toString().take(9)}",
                alternatePhone = if (index % 3 == 0) "+48${(600000000 + index * 111111).toString().take(9)}" else null,
                address = AddressEmbeddable(
                    street = streets[index % streets.size],
                    houseNumber = "${(index + 1) * 3}",
                    apartmentNumber = if (index % 2 == 0) "${index + 10}" else null,
                    postalCode = "6${index % 2}-${String.format("%03d", (100 + index * 10) % 1000)}",
                    city = cities[index % cities.size]
                ),
                communicationPreference = CommunicationPreference.values()[index % CommunicationPreference.values().size]
            )
            guardianRepository.save(guardian)
        }
    }

    private fun createChildren(companyId: String): List<ChildEntity> {
        val childData = listOf(
            Triple("Zosia", "Kowalska", LocalDate.of(2017, 3, 15)),
            Triple("Jaś", "Nowak", LocalDate.of(2018, 7, 22)),
            Triple("Ania", "Wiśniewska", LocalDate.of(2016, 11, 8)),
            Triple("Kacper", "Wójcik", LocalDate.of(2017, 5, 30)),
            Triple("Maja", "Kamińska", LocalDate.of(2019, 1, 12)),
            Triple("Filip", "Lewandowski", LocalDate.of(2018, 9, 5)),
            Triple("Oliwia", "Zielińska", LocalDate.of(2017, 12, 20)),
            Triple("Szymon", "Szymański", LocalDate.of(2016, 4, 17)),
            Triple("Julia", "Dąbrowska", LocalDate.of(2018, 6, 25)),
            Triple("Michał", "Kozłowski", LocalDate.of(2019, 2, 14)),
            Triple("Natalia", "Jankowska", LocalDate.of(2017, 8, 9)),
            Triple("Jakub", "Mazur", LocalDate.of(2018, 10, 3)),
            Triple("Wiktoria", "Krawczyk", LocalDate.of(2016, 12, 28)),
            Triple("Adam", "Piotrowski", LocalDate.of(2019, 3, 7)),
            Triple("Emilia", "Grabowska", LocalDate.of(2017, 7, 19)),
            Triple("Aleksander", "Kowalski", LocalDate.of(2018, 11, 11)),
            Triple("Lena", "Nowacka", LocalDate.of(2016, 5, 23)),
            Triple("Antoni", "Wiśniewski", LocalDate.of(2019, 4, 16)),
            Triple("Zuzanna", "Wójcik", LocalDate.of(2017, 9, 2)),
            Triple("Nikodem", "Kamiński", LocalDate.of(2018, 1, 29))
        )

        val disabilities = listOf(
            setOf<DisabilityType>(),
            setOf(DisabilityType.PHYSICAL),
            setOf(DisabilityType.INTELLECTUAL),
            setOf(DisabilityType.SENSORY_VISUAL),
            setOf(DisabilityType.AUTISM),
            setOf<DisabilityType>(),
            setOf(DisabilityType.PHYSICAL, DisabilityType.SENSORY_VISUAL),
            setOf<DisabilityType>(),
            setOf(DisabilityType.INTELLECTUAL),
            setOf<DisabilityType>(),
            setOf(DisabilityType.AUTISM),
            setOf<DisabilityType>(),
            setOf(DisabilityType.PHYSICAL),
            setOf<DisabilityType>(),
            setOf(DisabilityType.PHYSICAL),
            setOf<DisabilityType>(),
            setOf(DisabilityType.INTELLECTUAL),
            setOf<DisabilityType>(),
            setOf(DisabilityType.AUTISM),
            setOf<DisabilityType>()
        )

        return childData.mapIndexed { index, (firstName, lastName, birthDate) ->
            val needsWheelchair = disabilities[index].contains(DisabilityType.PHYSICAL) && index % 4 == 0
            val needsSpecialSeat = index % 3 == 0
            val needsAssistant = disabilities[index].isNotEmpty() && index % 5 == 0

            val child = ChildEntity(
                id = "CH-${UUID.randomUUID()}",
                companyId = companyId,
                firstName = firstName,
                lastName = lastName,
                birthDate = birthDate,
                status = ChildStatus.ACTIVE,
                disability = disabilities[index],
                transportNeeds = TransportNeeds(
                    wheelchair = needsWheelchair,
                    specialSeat = needsSpecialSeat,
                    safetyBelt = false,
                ),
                notes = if (index % 4 == 0) "Dodatkowe uwagi dla dziecka #${index + 1}" else null
            )
            childRepository.save(child)
        }
    }

    private fun assignGuardiansToChildren(
        companyId: String,
        guardians: List<GuardianEntity>,
        children: List<ChildEntity>
    ) {
        children.forEachIndexed { index, child ->
            // Główny opiekun (parent)
            val primaryGuardianIndex = index % guardians.size
            val primaryAssignment = GuardianAssignmentEntity(
                id = "GA-${UUID.randomUUID()}",
                companyId = companyId,
                guardianId = guardians[primaryGuardianIndex].id,
                childId = child.id,
                relationship = GuardianRelationship.PARENT,
                isPrimary = true,
                canPickup = true,
                canAuthorize = true
            )
            guardianAssignmentRepository.save(primaryAssignment)

            // Drugi opiekun (co drugie dziecko)
            if (index % 2 == 0) {
                val secondaryGuardianIndex = (index + 1) % guardians.size
                val secondaryAssignment = GuardianAssignmentEntity(
                    id = "GA-${UUID.randomUUID()}",
                    companyId = companyId,
                    guardianId = guardians[secondaryGuardianIndex].id,
                    childId = child.id,
                    relationship = if (index % 4 == 0) GuardianRelationship.PARENT else GuardianRelationship.GRANDPARENT,
                    isPrimary = false,
                    canPickup = true,
                    canAuthorize = index % 4 == 0
                )
                guardianAssignmentRepository.save(secondaryAssignment)
            }
        }
    }

    private fun createSchedules(companyId: String, children: List<ChildEntity>) {
        val schoolAddresses = listOf(
            Triple("Szkoła Podstawowa nr 1", "ul. Szkolna", "Poznań"),
            Triple("Szkoła Podstawowa nr 5", "ul. Słoneczna", "Poznań"),
            Triple("Przedszkole Wesoły Domek", "ul. Bajkowa", "Poznań"),
            Triple("Ośrodek Rehabilitacji", "ul. Zdrowia", "Poznań")
        )

        val homeStreets = listOf(
            "ul. Słowackiego", "ul. Mickiewicza", "ul. Kościuszki", "ul. Piłsudskiego",
            "ul. Sienkiewicza", "ul. Żeromskiego", "ul. Prusa", "ul. Reymonta",
            "ul. Konopnickiej", "ul. Orzeszkowej", "ul. Kraszewskiego", "ul. Wyspiańskiego",
            "ul. Norwida", "ul. Fredry", "ul. Staffa", "ul. Matejki", "ul. Chopina",
            "ul. Grunwaldzka", "ul. Armii Krajowej", "ul. Polna"
        )

        children.forEachIndexed { index, child ->
            val schoolAddress = schoolAddresses[index % schoolAddresses.size]
            val homeStreet = homeStreets[index % homeStreets.size]

            // Harmonogram 1: Poniedziałek - Środa (rano do szkoły)
            val schedule1 = ScheduleEntity(
                id = "SCH-${UUID.randomUUID()}",
                companyId = companyId,
                childId = child.id,
                name = "Poniedziałek-Środa - Rano",
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                pickupTime = LocalTime.of(7, 12 + (index % 4) * 10),
                pickupAddressLabel = "Dom",
                pickupAddressStreet = homeStreet,
                pickupAddressHouseNumber = "${(index + 1) * 5}",
                pickupAddressApartmentNumber = if (index % 3 == 0) "${index + 10}" else null,
                pickupAddressPostalCode = "61-${String.format("%03d", (100 + index * 5) % 1000)}",
                pickupAddressCity = "Poznań",
                pickupLatitude = 52.4064 + (index * 0.001),
                pickupLongitude = 16.9252 + (index * 0.001),
                dropoffTime = LocalTime.of(8, 15 + (index % 4) * 10),
                dropoffAddressLabel = schoolAddress.first,
                dropoffAddressStreet = schoolAddress.second,
                dropoffAddressHouseNumber = "${10 + index % 5}",
                dropoffAddressApartmentNumber = null,
                dropoffAddressPostalCode = "61-${String.format("%03d", (200 + index * 3) % 1000)}",
                dropoffAddressCity = schoolAddress.third,
                dropoffLatitude = 52.4164 + (index * 0.001),
                dropoffLongitude = 16.9352 + (index * 0.001),
                specialInstructions = if (index % 5 == 0) "Dziecko wymaga pomocy przy wsiadaniu" else null,
                active = true
            )
            scheduleRepository.save(schedule1)

            // Harmonogram 3: Czwartek - Piątek (rano)
            val schedule3 = ScheduleEntity(
                id = "SCH-${UUID.randomUUID()}",
                companyId = companyId,
                childId = child.id,
                name = "Czwartek-Piątek - Rano",
                days = setOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                pickupTime = LocalTime.of(7, 10 + (index % 4) * 10),
                pickupAddressLabel = "Dom",
                pickupAddressStreet = homeStreet,
                pickupAddressHouseNumber = "${(index + 1) * 5}",
                pickupAddressApartmentNumber = if (index % 3 == 0) "${index + 10}" else null,
                pickupAddressPostalCode = "61-${String.format("%03d", (100 + index * 5) % 1000)}",
                pickupAddressCity = "Poznań",
                pickupLatitude = 52.4064 + (index * 0.001),
                pickupLongitude = 16.9252 + (index * 0.001),
                dropoffTime = LocalTime.of(8, 12 + (index % 4) * 10),
                dropoffAddressLabel = schoolAddress.first,
                dropoffAddressStreet = schoolAddress.second,
                dropoffAddressHouseNumber = "${10 + index % 5}",
                dropoffAddressApartmentNumber = null,
                dropoffAddressPostalCode = "61-${String.format("%03d", (200 + index * 3) % 1000)}",
                dropoffAddressCity = schoolAddress.third,
                dropoffLatitude = 52.4164 + (index * 0.001),
                dropoffLongitude = 16.9352 + (index * 0.001),
                specialInstructions = null,
                active = true
            )
            scheduleRepository.save(schedule3)

            // Harmonogram 4: Czwartek - Piątek (po południu) - USUNIĘTY
        }
    }
        private fun createVehicles(companyId: String): List<VehicleEntity> {
            val vehicleData = listOf(
                // Busy
                Triple("Mercedes-Benz", "Sprinter 519", VehicleType.BUS),
                Triple("Volkswagen", "Crafter", VehicleType.BUS),
                Triple("Ford", "Transit", VehicleType.BUS),
                Triple("Iveco", "Daily", VehicleType.BUS),
                // Mikrobusy
                Triple("Mercedes-Benz", "Vito", VehicleType.MICROBUS),
                Triple("Volkswagen", "Caravelle", VehicleType.MICROBUS),
                Triple("Renault", "Trafic", VehicleType.MICROBUS),
                // Vany
                Triple("Fiat", "Ducato", VehicleType.VAN),
                Triple("Peugeot", "Boxer", VehicleType.VAN),
                Triple("Citroën", "Jumper", VehicleType.VAN)
            )

            val registrationNumbers = listOf(
                "PO 12345", "PO 23456", "PO 34567", "PO 45678", "PO 56789",
                "PO 67890", "PO 78901", "PO 89012", "PO 90123", "PO 01234"
            )

            return vehicleData.mapIndexed { index, (make, model, vehicleType) ->
                val (totalSeats, wheelchairSpaces, childSeats) = when (vehicleType) {
                    VehicleType.BUS -> Triple(16, 2, 10)
                    VehicleType.MICROBUS -> Triple(9, 1, 6)
                    VehicleType.VAN -> Triple(6, 1, 4)
                }

                val vehicle = Vehicle.create(
                    companyId = CompanyId(companyId),
                    registrationNumber = registrationNumbers[index],
                    make = make,
                    model = model,
                    year = 2018 + (index % 5),
                    vehicleType = vehicleType,
                    capacity = VehicleCapacity(
                        totalSeats = totalSeats,
                        wheelchairSpaces = wheelchairSpaces,
                        childSeats = childSeats
                    ),
                    specialEquipment = buildSet {
                        add("Klimatyzacja")
                        add("Monitoring wewnętrzny")
                        if (wheelchairSpaces > 0) {
                            add("Platforma dla wózków")
                            add("Rampa")
                        }
                        if (index % 2 == 0) {
                            add("GPS")
                            add("System łączności")
                        }
                    },
                    insurance = Insurance(
                        policyNumber = "POL/${2024 + index}/ABC/${String.format("%06d", 100000 + index * 1111)}",
                        validUntil = LocalDate.now().plusYears(1),
                        insurer = if (index % 2 == 0) "PZU" else "Warta"
                    ),
                    technicalInspection = TechnicalInspection(
                        validUntil = LocalDate.now().plusMonths((6 + (index % 6)).toLong()),
                        inspectionStation = "SKP Poznań ${index % 3 + 1}"
                    ),
                    vin = "VF1${String.format("%014d", 10000000000000L + index * 123456789L)}"
                )
                vehicleRepository.save(VehicleEntity.fromDomain(vehicle))
        }
    }

    private fun createDrivers(companyId: String): List<DriverEntity> {
        val driverData = listOf(
            Triple("Andrzej", "Kowalski", "andrzej.kowalski@transport.com"),
            Triple("Barbara", "Nowak", "barbara.nowak@transport.com"),
            Triple("Czesław", "Wiśniewski", "czeslaw.wisniewski@transport.com"),
            Triple("Danuta", "Wójcik", "danuta.wojcik@transport.com"),
            Triple("Edward", "Kamiński", "edward.kaminski@transport.com"),
            Triple("Franciszek", "Lewandowski", "franciszek.lewandowski@transport.com"),
            Triple("Grażyna", "Zielińska", "grazyna.zielinska@transport.com"),
            Triple("Henryk", "Szymański", "henryk.szymanski@transport.com")
        )

        val streets = listOf(
            "ul. Transportowa", "ul. Kierowców", "ul. Dostawcza", "ul. Logistyczna",
            "ul. Przewozowa", "ul. Ciężarowa", "ul. Autobusowa", "ul. Drogowa"
        )

        return driverData.mapIndexed { index, (firstName, lastName, email) ->
            val driver = Driver.create(
                companyId = CompanyId(companyId),
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = "+48${(700000000 + index * 111111).toString().take(9)}",
                dateOfBirth = LocalDate.of(1970 + index, 1 + (index % 12), 1 + (index % 28)),
                address = Address(
                    street = streets[index % streets.size],
                    houseNumber = "${(index + 1) * 7}",
                    apartmentNumber = if (index % 2 == 0) "${index + 20}" else null,
                    postalCode = "62-${String.format("%03d", (100 + index * 12) % 1000)}",
                    city = "Poznań"
                ),
                drivingLicense = DrivingLicense(
                    licenseNumber = "PRA${String.format("%07d", 1000000 + index * 123456)}",
                    categories = setOf("B", "D", "D1"),
                    validUntil = LocalDate.now().plusYears(5)
                ),
                medicalCertificate = MedicalCertificate(
                    validUntil = LocalDate.now().plusYears(1),
                    issueDate = LocalDate.now().minusMonths(6)
                )
            )
            driverJpaRepository.save(DriverEntity.fromDomain(driver))
        }
    }
}