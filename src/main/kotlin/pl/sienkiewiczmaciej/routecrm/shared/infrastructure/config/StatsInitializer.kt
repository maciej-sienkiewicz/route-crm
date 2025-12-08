package pl.sienkiewiczmaciej.routecrm.shared.infrastructure.config

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildJpaRepository
import pl.sienkiewiczmaciej.routecrm.driver.infrastructure.DriverJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.domain.ExecutionStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import pl.sienkiewiczmaciej.routecrm.route.domain.StopType
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteEntity
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopEntity
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopJpaRepository
import pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.statistics.application.DailyMetricsAggregationService
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository
import pl.sienkiewiczmaciej.routecrm.vehicle.infrastructure.VehicleJpaRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random

@Component
@Order(2)
class StatsInitializer(
    private val companyRepository: CompanyJpaRepository,
    private val driverRepository: DriverJpaRepository,
    private val vehicleRepository: VehicleJpaRepository,
    private val childRepository: ChildJpaRepository,
    private val scheduleRepository: ScheduleJpaRepository,
    private val routeRepository: RouteJpaRepository,
    private val routeStopRepository: RouteStopJpaRepository,
    private val aggregationService: DailyMetricsAggregationService
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) = runBlocking {
        initializeStatisticsData()
    }

    private suspend fun initializeStatisticsData() {
        logger.info("Initializing statistics test data...")

        val companies = companyRepository.findAll()
        if (companies.isEmpty()) {
            logger.warn("No companies found. Skipping statistics initialization.")
            return
        }

        val company = companies.first()
        val companyId = company.id

        val drivers = driverRepository.findByCompanyId(
            companyId,
            org.springframework.data.domain.Pageable.unpaged()
        ).content

        val vehicles = vehicleRepository.findByCompanyId(
            companyId,
            org.springframework.data.domain.Pageable.unpaged()
        ).content

        val children = childRepository.findAll().filter { it.companyId == companyId }
        val schedules = scheduleRepository.findAll().filter { it.companyId == companyId && it.active }

        if (drivers.isEmpty() || vehicles.isEmpty() || children.isEmpty() || schedules.isEmpty()) {
            logger.warn("Insufficient data for statistics initialization. Skipping.")
            return
        }

        val today = LocalDate.now()
        val daysToGenerate = 30
        var totalRoutesCreated = 0

        for (daysAgo in 1..daysToGenerate) {
            val date = today.minusDays(daysAgo.toLong())
            val dayOfWeek = date.dayOfWeek

            val routesForDay = generateRoutesForDay(
                companyId = companyId,
                date = date,
                drivers = drivers.map { it.id },
                vehicles = vehicles.map { it.id },
                children = children,
                schedules = schedules,
                dayOfWeek = convertDayOfWeek(dayOfWeek)
            )

            totalRoutesCreated += routesForDay
            logger.info("Generated $routesForDay routes for $date")

            try {
                aggregationService.aggregateMetricsForDate(CompanyId(companyId), date)
                logger.info("Aggregated metrics for $date")
            } catch (e: Exception) {
                logger.error("Failed to aggregate metrics for $date", e)
            }
        }

        logger.info("""
            |
            |===========================================
            | Statistics data created successfully!
            |===========================================
            | Total routes created: $totalRoutesCreated
            | Days with data: $daysToGenerate
            | Metrics aggregated: YES
            |===========================================
            |
        """.trimMargin())
    }

    private fun generateRoutesForDay(
        companyId: String,
        date: LocalDate,
        drivers: List<String>,
        vehicles: List<String>,
        children: List<pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildEntity>,
        schedules: List<pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleEntity>,
        dayOfWeek: pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek
    ): Int {
        val availableSchedules = schedules.filter { it.days.contains(dayOfWeek) }
        if (availableSchedules.isEmpty()) return 0

        val availableChildren = children.filter { child ->
            availableSchedules.any { it.childId == child.id }
        }

        if (availableChildren.isEmpty()) return 0

        val routesCount = Random.nextInt(5, 9)
        var routesCreated = 0

        val shuffledDrivers = drivers.shuffled()
        val shuffledVehicles = vehicles.shuffled()

        val childrenPerRoute = availableChildren.chunked(
            (availableChildren.size / routesCount).coerceAtLeast(3)
        )

        childrenPerRoute.take(routesCount).forEachIndexed { routeIndex, routeChildren ->
            if (routeIndex >= shuffledDrivers.size || routeIndex >= shuffledVehicles.size) return@forEachIndexed

            val driverId = shuffledDrivers[routeIndex]
            val vehicleId = shuffledVehicles[routeIndex]

            val startHour = if (routeIndex % 2 == 0) 7 else 14
            val estimatedStartTime = LocalTime.of(startHour, Random.nextInt(0, 30))
            val estimatedEndTime = estimatedStartTime.plusMinutes(90 + Random.nextInt(-15, 30).toLong())

            val route = RouteEntity(
                id = "RT-STATS-${UUID.randomUUID()}",
                companyId = companyId,
                routeName = "Trasa ${if (startHour == 7) "Poranna" else "Popołudniowa"} ${('A' + routeIndex)}",
                date = date,
                status = RouteStatus.COMPLETED,
                driverId = driverId,
                vehicleId = vehicleId,
                estimatedStartTime = estimatedStartTime,
                estimatedEndTime = estimatedEndTime,
                actualStartTime = date.atTime(estimatedStartTime).plusMinutes(Random.nextInt(-5, 10).toLong())
                    .toInstant(ZoneOffset.UTC),
                actualEndTime = date.atTime(estimatedEndTime).plusMinutes(Random.nextInt(-10, 20).toLong())
                    .toInstant(ZoneOffset.UTC),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                seriesId = null,
                seriesOccurrenceDate = null
            )

            routeRepository.save(route)

            createStopsForRoute(
                companyId = companyId,
                routeId = route.id,
                date = date,
                children = routeChildren,
                schedules = availableSchedules,
                baseTime = estimatedStartTime.plusMinutes(15),
                driverName = "Driver-${routeIndex + 1}"
            )

            routesCreated++
        }

        return routesCreated
    }

    private fun createStopsForRoute(
        companyId: String,
        routeId: String,
        date: LocalDate,
        children: List<pl.sienkiewiczmaciej.routecrm.child.infrastructure.ChildEntity>,
        schedules: List<pl.sienkiewiczmaciej.routecrm.schedule.infrastructure.ScheduleEntity>,
        baseTime: LocalTime,
        driverName: String
    ): List<RouteStopEntity> {
        val stops = mutableListOf<RouteStopEntity>()
        var stopOrder = 1

        children.forEach { child ->
            val schedule = schedules.find { it.childId == child.id } ?: return@forEach

            val pickupTime = baseTime.plusMinutes((stopOrder * 8).toLong())
            val delayMinutes = generateDelayMinutes()
            val actualPickupTime = date.atTime(pickupTime).plusMinutes(delayMinutes.toLong())
                .toInstant(ZoneOffset.UTC)

            val pickupStop = RouteStopEntity(
                id = "ST-STATS-${UUID.randomUUID()}",
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
                actualTime = actualPickupTime,
                executionStatus = ExecutionStatus.COMPLETED,
                executionNotes = if (delayMinutes > 5) "Opóźnienie ${delayMinutes} min" else null,
                executedByUserId = "SYS-STATS",
                executedByName = driverName,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            stops.add(routeStopRepository.save(pickupStop))
        }

        children.forEach { child ->
            val schedule = schedules.find { it.childId == child.id } ?: return@forEach

            val dropoffTime = baseTime.plusMinutes((children.size * 8 + stopOrder * 6).toLong())
            val delayMinutes = generateDelayMinutes()
            val actualDropoffTime = date.atTime(dropoffTime).plusMinutes(delayMinutes.toLong())
                .toInstant(ZoneOffset.UTC)

            val dropoffStop = RouteStopEntity(
                id = "ST-STATS-${UUID.randomUUID()}",
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
                actualTime = actualDropoffTime,
                executionStatus = ExecutionStatus.COMPLETED,
                executionNotes = if (delayMinutes > 5) "Opóźnienie ${delayMinutes} min" else null,
                executedByUserId = "SYS-STATS",
                executedByName = driverName,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            stops.add(routeStopRepository.save(dropoffStop))
        }

        return stops
    }

    private fun generateDelayMinutes(): Int {
        val random = Random.nextInt(100)
        return when {
            random < 60 -> Random.nextInt(-2, 3)
            random < 80 -> Random.nextInt(3, 8)
            random < 95 -> Random.nextInt(8, 15)
            else -> Random.nextInt(15, 30)
        }
    }

    private fun convertDayOfWeek(dayOfWeek: java.time.DayOfWeek): pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek {
        return when (dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek.MONDAY
            java.time.DayOfWeek.TUESDAY -> pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek.THURSDAY
            java.time.DayOfWeek.FRIDAY -> pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek.FRIDAY
            java.time.DayOfWeek.SATURDAY -> pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek.SATURDAY
            java.time.DayOfWeek.SUNDAY -> pl.sienkiewiczmaciej.routecrm.schedule.domain.DayOfWeek.SUNDAY
        }
    }
}