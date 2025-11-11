// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteJpaRepositories.kt
package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import java.time.LocalDate
import java.time.LocalTime

// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/route/infrastructure/RouteJpaRepositories.kt

interface RouteJpaRepository : JpaRepository<RouteEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): RouteEntity?

    fun findByCompanyId(companyId: String, pageable: Pageable): Page<RouteEntity>

    // POPRAWIONE ZAPYTANIE - jawne rzutowanie typów
    @Query("""
        SELECT r FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND (CAST(:date AS date) IS NULL OR r.date = :date)
        AND (CAST(:status AS string) IS NULL OR r.status = :status)
        AND (CAST(:driverId AS string) IS NULL OR r.driverId = :driverId)
    """)
    fun findByFilters(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate?,
        @Param("status") status: RouteStatus?,
        @Param("driverId") driverId: String?,
        pageable: Pageable
    ): Page<RouteEntity>

    @Query("""
        SELECT r FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.driverId = :driverId
        AND (CAST(:date AS date) IS NULL OR r.date = :date)
    """)
    fun findByCompanyIdAndDriverIdAndDate(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("date") date: LocalDate?,
        pageable: Pageable
    ): Page<RouteEntity>

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.driverId = :driverId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND (CAST(:excludeRouteId AS string) IS NULL OR r.id != :excludeRouteId)
        AND (
            (r.estimatedStartTime < :endTime AND r.estimatedEndTime > :startTime)
        )
    """)
    fun existsDriverConflict(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("date") date: LocalDate,
        @Param("startTime") startTime: LocalTime,
        @Param("endTime") endTime: LocalTime,
        @Param("excludeRouteId") excludeRouteId: String?
    ): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.vehicleId = :vehicleId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND (CAST(:excludeRouteId AS string) IS NULL OR r.id != :excludeRouteId)
        AND (
            (r.estimatedStartTime < :endTime AND r.estimatedEndTime > :startTime)
        )
    """)
    fun existsVehicleConflict(
        @Param("companyId") companyId: String,
        @Param("vehicleId") vehicleId: String,
        @Param("date") date: LocalDate,
        @Param("startTime") startTime: LocalTime,
        @Param("endTime") endTime: LocalTime,
        @Param("excludeRouteId") excludeRouteId: String?
    ): Boolean

    // NOWE ZAPYTANIE dla history/upcoming
    @Query("""
        SELECT DISTINCT r FROM RouteEntity r
        JOIN RouteStopEntity s ON s.routeId = r.id AND s.companyId = r.companyId
        WHERE r.companyId = :companyId
        AND s.scheduleId = :scheduleId
        AND r.status IN :statuses
        ORDER BY r.date DESC, r.estimatedStartTime DESC
    """)
    fun findByCompanyIdAndScheduleIdAndStatuses(
        @Param("companyId") companyId: String,
        @Param("scheduleId") scheduleId: String,
        @Param("statuses") statuses: Set<RouteStatus>,
        pageable: Pageable
    ): Page<RouteEntity>
}


interface RouteStopJpaRepository : JpaRepository<RouteStopEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): RouteStopEntity?

    fun findByCompanyIdAndRouteIdOrderByStopOrder(
        companyId: String,
        routeId: String
    ): List<RouteStopEntity>

    @Query("""
        SELECT s FROM RouteStopEntity s
        WHERE s.companyId = :companyId
        AND s.routeId = :routeId
        AND (:includeCancelled = true OR s.isCancelled = false)
        ORDER BY s.stopOrder
    """)
    fun findByCompanyIdAndRouteId(
        @Param("companyId") companyId: String,
        @Param("routeId") routeId: String,
        @Param("includeCancelled") includeCancelled: Boolean
    ): List<RouteStopEntity>

    fun countByCompanyIdAndRouteId(companyId: String, routeId: String): Int

    @Modifying
    fun deleteByCompanyIdAndRouteId(companyId: String, routeId: String)

    @Query("""
        SELECT s FROM RouteStopEntity s
        JOIN RouteEntity r ON s.routeId = r.id
        WHERE s.companyId = :companyId
        AND s.childId = :childId
        AND r.date >= :startDate
        AND r.date <= :endDate
        ORDER BY r.date, s.stopOrder
    """)
    fun findByChildIdInDateRange(
        @Param("companyId") companyId: String,
        @Param("childId") childId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<RouteStopEntity>

    @Query("""
        SELECT s FROM RouteStopEntity s
        WHERE s.companyId = :companyId
        AND s.cancelledByAbsenceId = :absenceId
    """)
    fun findByCancelledByAbsenceId(
        @Param("companyId") companyId: String,
        @Param("absenceId") absenceId: String
    ): List<RouteStopEntity>

    fun findByCancelledByAbsenceId(absenceId: String): List<RouteStopEntity>
}

interface RouteNoteJpaRepository : JpaRepository<RouteNoteEntity, String> {
    fun findByCompanyIdAndRouteIdOrderByCreatedAtDesc(
        companyId: String,
        routeId: String
    ): List<RouteNoteEntity>

    @Modifying
    fun deleteByCompanyIdAndRouteId(companyId: String, routeId: String)
}