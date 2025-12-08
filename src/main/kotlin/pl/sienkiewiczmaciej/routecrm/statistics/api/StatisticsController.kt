package pl.sienkiewiczmaciej.routecrm.statistics.api

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import pl.sienkiewiczmaciej.routecrm.statistics.api.dto.StatisticsTrendsDto
import pl.sienkiewiczmaciej.routecrm.statistics.application.StatisticsQueryService
import java.time.LocalDate

@RestController
@RequestMapping("/api/statistics")
class StatisticsController(
    private val queryService: StatisticsQueryService
): BaseController() {
    @GetMapping("/trends/fleet")
    suspend fun getFleetTrends(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<StatisticsTrendsDto> {
        val principal = getPrincipal()


        val metrics = queryService.getFleetMetrics(principal.companyId, startDate, endDate)

        return ResponseEntity.ok(
            StatisticsTrendsDto(
                metric = "fleet",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                fleetMetrics = metrics
            )
        )
    }

    @GetMapping("/trends/capacity")
    suspend fun getCapacityTrends(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<StatisticsTrendsDto> {
        val principal = getPrincipal()

        val metrics = queryService.getCapacityMetrics(principal.companyId, startDate, endDate)

        return ResponseEntity.ok(
            StatisticsTrendsDto(
                metric = "capacity",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                capacityMetrics = metrics
            )
        )
    }

    @GetMapping("/trends/workload")
    suspend fun getWorkloadTrends(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<StatisticsTrendsDto> {
        val principal = getPrincipal()

        val metrics = queryService.getWorkloadMetrics(principal.companyId, startDate, endDate)

        return ResponseEntity.ok(
            StatisticsTrendsDto(
                metric = "workload",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                workloadMetrics = metrics
            )
        )
    }

    @GetMapping("/trends/passenger")
    suspend fun getPassengerTrends(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<StatisticsTrendsDto> {
        val principal = getPrincipal()

        val metrics = queryService.getPassengerMetrics(principal.companyId, startDate, endDate)

        return ResponseEntity.ok(
            StatisticsTrendsDto(
                metric = "passenger",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                passengerMetrics = metrics
            )
        )
    }

    @GetMapping("/drivers/{driverId}/performance")
    suspend fun getDriverPerformance(
        @PathVariable driverId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ) = ResponseEntity.ok(
        queryService.getDriverPerformance(
            getPrincipal().companyId,
            DriverId(driverId),
            startDate,
            endDate
        )
    )

    @GetMapping("/drivers/performance/ranking")
    suspend fun getDriversPerformanceRanking(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ) = ResponseEntity.ok(
        queryService.getAllDriversPerformance(
            getPrincipal().companyId,
            startDate,
            endDate
        )
    )
}