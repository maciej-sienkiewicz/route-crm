package pl.sienkiewiczmaciej.routecrm.shared.infrastructure.config

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.child.domain.DisabilityType
import pl.sienkiewiczmaciej.routecrm.child.domain.TransportNeeds
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildEntity
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.driver.domain.DrivingLicense
import pl.sienkiewiczmaciej.routecrm.driver.domain.MedicalCertificate
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverEntity
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.*
import pl.sienkiewiczmaciej.routecrm.route.domain.ExecutionStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteEntity
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopEntity
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopJpaRepository
import pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
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
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.*
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleEntity
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
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
    private val routeJpaRepository: RouteJpaRepository,
    private val routeStopJpaRepository: RouteStopJpaRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) = runBlocking {
        initializeTestData()
    }

    private suspend fun initializeTestData() {
        logger.info("Initializing test data...")

        val companyId = CompanyId.generate()
        val companyBefore = CompanyEntity(
            id = companyId.value,
            name = "Demo Transport Company",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val company = companyRepository.save(companyBefore)
        logger.info("Created test company: ${company.name} (${company.id})")

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

        val guardians = createGuardians(companyId.value)
        logger.info("Created ${guardians.size} guardians")

        val children = createChildren(companyId.value)
        logger.info("Created ${children.size} children")

        assignGuardiansToChildren(companyId.value, guardians, children)
        logger.info("Created guardian assignments")

        val drivers = createDrivers(companyId.value)
        logger.info("Created ${drivers.size} drivers")

        val vehicles = createVehicles(companyId.value)
        logger.info("Created ${vehicles.size} vehicles")

        val schedules = createSchedules(companyId.value, children)
        logger.info("Created ${schedules.size} schedules")

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
            | Data Summary:
            |   Guardians: ${guardians.size}
            |   Children: ${children.size}
            |   Drivers: ${drivers.size}
            |   Vehicles: ${vehicles.size}
            |   Schedules: ${schedules.size}
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
                address = AddressEmbeddable(
                    street = streets[index % streets.size],
                    houseNumber = "${(index + 1) * 3}",
                    apartmentNumber = if (index % 2 == 0) "${index + 10}" else null,
                    postalCode = "6${index % 2}-${String.format("%03d", (100 + index * 10) % 1000)}",
                    city = cities[index % cities.size]
                ),
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

    private fun createSchedules(companyId: String, children: List<ChildEntity>): List<ScheduleEntity> {
        val schedules = mutableListOf<ScheduleEntity>()

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
            schedules.add(scheduleRepository.save(schedule1))

            val schedule2 = ScheduleEntity(
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
            schedules.add(scheduleRepository.save(schedule2))
        }

        return schedules
    }

    private fun createVehicles(companyId: String): List<VehicleEntity> {
        val vehicleData = listOf(
            Triple("Mercedes-Benz", "Sprinter 519", VehicleType.BUS),
            Triple("Volkswagen", "Crafter", VehicleType.BUS),
            Triple("Ford", "Transit", VehicleType.BUS),
            Triple("Iveco", "Daily", VehicleType.BUS),
            Triple("Mercedes-Benz", "Vito", VehicleType.MICROBUS),
            Triple("Volkswagen", "Caravelle", VehicleType.MICROBUS),
            Triple("Renault", "Trafic", VehicleType.MICROBUS),
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

    private fun createRoutes(
        companyId: String,
        drivers: List<DriverEntity>,
        vehicles: List<VehicleEntity>,
        children: List<ChildEntity>,
        schedules: List<ScheduleEntity>
    ): List<RouteEntity> {
        val routes = mutableListOf<RouteEntity>()

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val yesterday = today.minusDays(1)

        val mondaySchedules = schedules.filter { it.days.contains(DayOfWeek.MONDAY) }
        val childrenForMonday = children.filter { child ->
            mondaySchedules.any { it.childId == child.id }
        }.take(6)

        if (childrenForMonday.isNotEmpty()) {
            val route1 = createSampleRoute(
                companyId = companyId,
                routeName = "Poranek Poniedziałek A",
                date = tomorrow,
                driverId = drivers[0].id,
                vehicleId = vehicles[0].id,
                estimatedStartTime = LocalTime.of(7, 0),
                estimatedEndTime = LocalTime.of(9, 0),
                status = RouteStatus.PLANNED
            )
            routes.add(routeJpaRepository.save(route1))

            createStopsForRoute(
                companyId = companyId,
                routeId = route1.id,
                children = childrenForMonday,
                schedules = schedules,
                baseTime = LocalTime.of(7, 15)
            )
        }

        val tuesdaySchedules = schedules.filter { it.days.contains(DayOfWeek.TUESDAY) }
        val childrenForTuesday = children.filter {
                child ->
            tuesdaySchedules.any { it.childId == child.id }
        }.take(8)

        if (childrenForTuesday.isNotEmpty()) {
            val route2 = createSampleRoute(
                companyId = companyId,
                routeName = "Poranek Wtorek A",
                date = today,
                driverId = drivers[1].id,
                vehicleId = vehicles[1].id,
                estimatedStartTime = LocalTime.of(7, 0),
                estimatedEndTime = LocalTime.of(9, 0),
                status = RouteStatus.IN_PROGRESS,
                actualStartTime = Instant.now().minusSeconds(3600)
            )
            routes.add(routeJpaRepository.save(route2))

            val stops = createStopsForRoute(
                companyId = companyId,
                routeId = route2.id,
                children = childrenForTuesday,
                schedules = schedules,
                baseTime = LocalTime.of(7, 10)
            )

            if (stops.size >= 4) {
                val stop1 = stops[0]
                routeStopJpaRepository.save(
                    stop1.copy(
                        actualTime = Instant.now().minusSeconds(2400),
                        executionStatus = ExecutionStatus.COMPLETED,
                        executedByUserId = "SYS-INIT",
                        executedByName = "${drivers[1].firstName} ${drivers[1].lastName}"
                    )
                )

                val stop2 = stops[1]
                routeStopJpaRepository.save(
                    stop2.copy(
                        actualTime = Instant.now().minusSeconds(1800),
                        executionStatus = ExecutionStatus.COMPLETED,
                        executedByUserId = "SYS-INIT",
                        executedByName = "${drivers[1].firstName} ${drivers[1].lastName}"
                    )
                )
            }
        }

        val wednesdaySchedules = schedules.filter { it.days.contains(DayOfWeek.WEDNESDAY) }
        val childrenForWednesday = children.filter { child ->
            wednesdaySchedules.any { it.childId == child.id }
        }.take(5)

        if (childrenForWednesday.isNotEmpty()) {
            val route3 = createSampleRoute(
                companyId = companyId,
                routeName = "Poranek Środa B",
                date = yesterday,
                driverId = drivers[2].id,
                vehicleId = vehicles[2].id,
                estimatedStartTime = LocalTime.of(7, 0),
                estimatedEndTime = LocalTime.of(8, 45),
                status = RouteStatus.COMPLETED,
                actualStartTime = yesterday.atTime(7, 2).toInstant(ZoneOffset.UTC),
                actualEndTime = yesterday.atTime(8, 50).toInstant(ZoneOffset.UTC)
            )
            routes.add(routeJpaRepository.save(route3))

            val stops = createStopsForRoute(
                companyId = companyId,
                routeId = route3.id,
                children = childrenForWednesday,
                schedules = schedules,
                baseTime = LocalTime.of(7, 15)
            )

            stops.forEach { stop ->
                routeStopJpaRepository.save(
                    stop.copy(
                        actualTime = yesterday.atTime(stop.estimatedTime).plusMinutes(2)
                            .toInstant(ZoneOffset.UTC),
                        executionStatus = ExecutionStatus.COMPLETED,
                        executedByUserId = "SYS-INIT",
                        executedByName = "${drivers[2].firstName} ${drivers[2].lastName}"
                    )
                )
            }
        }

        val thursdaySchedules = schedules.filter { it.days.contains(DayOfWeek.THURSDAY) }
        val childrenForThursday = children.filter { child ->
            thursdaySchedules.any { it.childId == child.id }
        }.take(7)

        if (childrenForThursday.isNotEmpty()) {
            val route4 = createSampleRoute(
                companyId = companyId,
                routeName = "Popołudnie Czwartek A",
                date = tomorrow,
                driverId = drivers[3].id,
                vehicleId = vehicles[3].id,
                estimatedStartTime = LocalTime.of(14, 0),
                estimatedEndTime = LocalTime.of(16, 0),
                status = RouteStatus.PLANNED
            )
            routes.add(routeJpaRepository.save(route4))

            createStopsForRoute(
                companyId = companyId,
                routeId = route4.id,
                children = childrenForThursday,
                schedules = schedules,
                baseTime = LocalTime.of(14, 15),
                isAfternoon = true
            )
        }

        val fridaySchedules = schedules.filter { it.days.contains(DayOfWeek.FRIDAY) }
        val childrenForFriday = children.filter { child ->
            fridaySchedules.any { it.childId == child.id }
        }.take(4)

        if (childrenForFriday.isNotEmpty()) {
            val route5 = createSampleRoute(
                companyId = companyId,
                routeName = "Poranek Piątek C",
                date = today,
                driverId = drivers[4].id,
                vehicleId = vehicles[4].id,
                estimatedStartTime = LocalTime.of(7, 30),
                estimatedEndTime = LocalTime.of(9, 15),
                status = RouteStatus.PLANNED
            )
            routes.add(routeJpaRepository.save(route5))

            val stops = createStopsForRoute(
                companyId = companyId,
                routeId = route5.id,
                children = childrenForFriday,
                schedules = schedules,
                baseTime = LocalTime.of(7, 45)
            )

            if (stops.isNotEmpty()) {
                val cancelledStop = stops.last()
                routeStopJpaRepository.save(
                    cancelledStop.copy(
                        isCancelled = true,
                        cancelledAt = Instant.now().minusSeconds(7200),
                        cancellationReason = "Dziecko chore - nie przyjedzie dziś"
                    )
                )
            }
        }

        return routes
    }

    private fun createSampleRoute(
        companyId: String,
        routeName: String,
        date: LocalDate,
        driverId: String,
        vehicleId: String,
        estimatedStartTime: LocalTime,
        estimatedEndTime: LocalTime,
        status: RouteStatus,
        actualStartTime: Instant? = null,
        actualEndTime: Instant? = null
    ): RouteEntity {
        return RouteEntity(
            id = "RT-${UUID.randomUUID()}",
            companyId = companyId,
            routeName = routeName,
            date = date,
            status = status,
            driverId = driverId,
            vehicleId = vehicleId,
            estimatedStartTime = estimatedStartTime,
            estimatedEndTime = estimatedEndTime,
            actualStartTime = actualStartTime,
            actualEndTime = actualEndTime,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            seriesId = null,
            seriesOccurrenceDate = null,
        )
    }

    private fun createStopsForRoute(
        companyId: String,
        routeId: String,
        children: List<ChildEntity>,
        schedules: List<ScheduleEntity>,
        baseTime: LocalTime,
        isAfternoon: Boolean = false
    ): List<RouteStopEntity> {
        val stops = mutableListOf<RouteStopEntity>()
        var stopOrder = 1

        children.forEachIndexed { index, child ->
            val childSchedules = schedules.filter { it.childId == child.id && it.active }
            if (childSchedules.isEmpty()) return@forEachIndexed

            val schedule = childSchedules.first()

            val pickupTime = baseTime.plusMinutes((index * 10).toLong())
            val pickupStop = RouteStopEntity(
                id = "ST-${UUID.randomUUID()}",
                companyId = companyId,
                routeId = routeId,
                stopOrder = stopOrder++,
                stopType = StopType.PICKUP,
                childId = child.id,
                scheduleId = schedule.id,
                estimatedTime = pickupTime,
                addressLabel = schedule.pickupAddressLabel,
                addressStreet = schedule.pickupAddressStreet,
                addressHouseNumber = schedule.pickupAddressHouseNumber,
                addressApartmentNumber = schedule.pickupAddressApartmentNumber,
                addressPostalCode = schedule.pickupAddressPostalCode,
                addressCity = schedule.pickupAddressCity,
                latitude = schedule.pickupLatitude,
                longitude = schedule.pickupLongitude,
                isCancelled = false,
                cancelledAt = null,
                cancellationReason = null,
                actualTime = null,
                executionStatus = null,
                executionNotes = null,
                executedByUserId = null,
                executedByName = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            stops.add(routeStopJpaRepository.save(pickupStop))
        }

        children.forEachIndexed { index, child ->
            val childSchedules = schedules.filter { it.childId == child.id && it.active }
            if (childSchedules.isEmpty()) return@forEachIndexed

            val schedule = childSchedules.first()

            val dropoffTime = baseTime.plusMinutes((children.size * 10 + index * 8).toLong())
            val dropoffStop = RouteStopEntity(
                id = "ST-${UUID.randomUUID()}",
                companyId = companyId,
                routeId = routeId,
                stopOrder = stopOrder++,
                stopType = StopType.DROPOFF,
                childId = child.id,
                scheduleId = schedule.id,
                estimatedTime = dropoffTime,
                addressLabel = schedule.dropoffAddressLabel,
                addressStreet = schedule.dropoffAddressStreet,
                addressHouseNumber = schedule.dropoffAddressHouseNumber,
                addressApartmentNumber = schedule.dropoffAddressApartmentNumber,
                addressPostalCode = schedule.dropoffAddressPostalCode,
                addressCity = schedule.dropoffAddressCity,
                latitude = schedule.dropoffLatitude,
                longitude = schedule.dropoffLongitude,
                isCancelled = false,
                cancelledAt = null,
                cancellationReason = null,
                actualTime = null,
                executionStatus = null,
                executionNotes = null,
                executedByUserId = null,
                executedByName = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            stops.add(routeStopJpaRepository.save(dropoffStop))
        }

        return stops
    }
}
