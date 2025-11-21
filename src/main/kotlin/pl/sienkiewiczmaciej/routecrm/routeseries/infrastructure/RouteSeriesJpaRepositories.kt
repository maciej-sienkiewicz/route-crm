// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeseries/infrastructure/RouteSeriesJpaRepositories.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.routeseries.domain.RouteSeriesStatus
import java.time.LocalDate

interface RouteSeriesJpaRepository : JpaRepository<RouteSeriesEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): RouteSeriesEntity?

    fun findByCompanyId(companyId: String, pageable: Pageable): Page<RouteSeriesEntity>

    fun findByCompanyIdAndStatus(
        companyId: String,
        status: RouteSeriesStatus,
        pageable: Pageable
    ): Page<RouteSeriesEntity>

    @Query("""
        SELECT s FROM RouteSeriesEntity s
        WHERE s.companyId = :companyId
        AND s.status = 'ACTIVE'
        AND s.startDate <= :endDate
        AND (s.endDate IS NULL OR s.endDate >= :startDate)
    """)
    fun findActiveInDateRange(
        @Param("companyId") companyId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<RouteSeriesEntity>
}

interface RouteSeriesScheduleJpaRepository : JpaRepository<RouteSeriesScheduleEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): RouteSeriesScheduleEntity?

    @Query("""
        SELECT s FROM RouteSeriesScheduleEntity s
        WHERE s.companyId = :companyId
        AND s.seriesId = :seriesId
        AND s.scheduleId = :scheduleId
        AND s.validFrom <= :date
        AND (s.validTo IS NULL OR s.validTo >= :date)
    """)
    fun findActiveBySeriesAndScheduleOnDate(
        @Param("companyId") companyId: String,
        @Param("seriesId") seriesId: String,
        @Param("scheduleId") scheduleId: String,
        @Param("date") date: LocalDate
    ): RouteSeriesScheduleEntity?

    @Query("""
        SELECT s FROM RouteSeriesScheduleEntity s
        WHERE s.companyId = :companyId
        AND s.seriesId = :seriesId
        AND s.validFrom <= :date
        AND (s.validTo IS NULL OR s.validTo >= :date)
        ORDER BY s.pickupStopOrder
    """)
    fun findActiveBySeriesOnDate(
        @Param("companyId") companyId: String,
        @Param("seriesId") seriesId: String,
        @Param("date") date: LocalDate
    ): List<RouteSeriesScheduleEntity>

    fun findByCompanyIdAndSeriesId(
        companyId: String,
        seriesId: String
    ): List<RouteSeriesScheduleEntity>
}

interface RouteSeriesOccurrenceJpaRepository : JpaRepository<RouteSeriesOccurrenceEntity, String> {
    fun findByCompanyIdAndSeriesIdAndOccurrenceDate(
        companyId: String,
        seriesId: String,
        occurrenceDate: LocalDate
    ): RouteSeriesOccurrenceEntity?

    @Query("""
        SELECT o FROM RouteSeriesOccurrenceEntity o
        WHERE o.companyId = :companyId
        AND o.seriesId = :seriesId
        AND o.occurrenceDate >= :fromDate
        AND o.occurrenceDate <= :toDate
        ORDER BY o.occurrenceDate
    """)
    fun findBySeriesInDateRange(
        @Param("companyId") companyId: String,
        @Param("seriesId") seriesId: String,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<RouteSeriesOccurrenceEntity>
}