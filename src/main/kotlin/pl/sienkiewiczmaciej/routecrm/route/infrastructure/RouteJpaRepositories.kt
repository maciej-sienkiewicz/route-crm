package pl.sienkiewiczmaciej.routecrm.route.infrastructure

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStatus
import java.time.LocalDate
import java.time.LocalTime

interface RouteJpaRepository : JpaRepository<RouteEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): RouteEntity?

    fun findByCompanyId(companyId: String, pageable: Pageable): Page<RouteEntity>

    @Query("""
        SELECT r FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND (:date IS NULL OR r.date = :date)
        AND (:status IS NULL OR r.status = :status)
        AND (:driverId IS NULL OR r.driverId = :driverId)
    """)
    fun findByFilters(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate?,
        @Param("status") status: RouteStatus?,
        @Param("driverId") driverId: String?,
        pageable: Pageable
    ): Page<RouteEntity>

    fun findByCompanyIdAndDriverId(
        companyId: String,
        driverId: String,
        pageable: Pageable
    ): Page<RouteEntity>

    @Query("""
        SELECT r FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.driverId = :driverId
        AND (:date IS NULL OR r.date = :date)
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
        AND (
            (r.estimatedStartTime < :endTime AND r.estimatedEndTime > :startTime)
        )
    """)
    fun existsDriverConflict(
        @Param("companyId") companyId: String,
        @Param("driverId") driverId: String,
        @Param("date") date: LocalDate,
        @Param("startTime") startTime: LocalTime,
        @Param("endTime") endTime: LocalTime
    ): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.vehicleId = :vehicleId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND (
            (r.estimatedStartTime < :endTime AND r.estimatedEndTime > :startTime)
        )
    """)
    fun existsVehicleConflict(
        @Param("companyId") companyId: String,
        @Param("vehicleId") vehicleId: String,
        @Param("date") date: LocalDate,
        @Param("startTime") startTime: LocalTime,
        @Param("endTime") endTime: LocalTime
    ): Boolean
}

interface RouteChildJpaRepository : JpaRepository<RouteChildEntity, String> {
    fun findByIdAndCompanyId(id: String, companyId: String): RouteChildEntity?

    fun findByCompanyIdAndRouteId(companyId: String, routeId: String): List<RouteChildEntity>

    fun findByCompanyIdAndRouteIdAndChildId(
        companyId: String,
        routeId: String,
        childId: String
    ): RouteChildEntity?

    fun countByCompanyIdAndRouteId(companyId: String, routeId: String): Int

    fun deleteByCompanyIdAndRouteId(companyId: String, routeId: String)

    @Query("""
        SELECT CASE WHEN COUNT(rc) > 0 THEN true ELSE false END
        FROM RouteChildEntity rc
        JOIN RouteEntity r ON rc.routeId = r.id
        WHERE rc.companyId = :companyId
        AND rc.childId = :childId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND (
            (rc.estimatedPickupTime < :dropoffTime AND rc.estimatedDropoffTime > :pickupTime)
        )
    """)
    fun existsChildConflict(
        @Param("companyId") companyId: String,
        @Param("childId") childId: String,
        @Param("date") date: LocalDate,
        @Param("pickupTime") pickupTime: LocalTime,
        @Param("dropoffTime") dropoffTime: LocalTime
    ): Boolean
}

interface RouteNoteJpaRepository : JpaRepository<RouteNoteEntity, String> {
    fun findByCompanyIdAndRouteIdOrderByCreatedAtDesc(
        companyId: String,
        routeId: String
    ): List<RouteNoteEntity>

    fun deleteByCompanyIdAndRouteId(companyId: String, routeId: String)
}