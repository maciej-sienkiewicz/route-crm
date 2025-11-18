// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/dashboard/infrastructure/DashboardJpaRepository.kt
package pl.sienkiewiczmaciej.routecrm.dashboard.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.ChildAlertItem
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.DriverDocAlertItem
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.RouteAlertItem
import pl.sienkiewiczmaciej.routecrm.dashboard.domain.VehicleDocAlertItem
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteEntity
import java.time.LocalDate

@Repository
interface DashboardJpaRepository : JpaRepository<RouteEntity, String> {

    // ============================================
    // READINESS QUERIES
    // ============================================

    @Query("""
        SELECT COUNT(r)
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND r.driverId IS NULL
    """)
    fun countRoutesWithoutDrivers(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate
    ): Int

    @Query("""
        SELECT COUNT(r)
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
    """)
    fun countTotalRoutes(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate
    ): Int

    @Query("""
        SELECT COUNT(r)
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND r.vehicleId IS NULL
    """)
    fun countRoutesWithoutVehicles(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate
    ): Int

    @Query("""
        SELECT COUNT(DISTINCT c.id)
        FROM ChildEntity c
        JOIN ScheduleEntity s ON s.childId = c.id AND s.companyId = c.companyId
        WHERE c.companyId = :companyId
        AND c.status = 'ACTIVE'
        AND s.active = true
        AND NOT EXISTS (
            SELECT 1
            FROM RouteStopEntity rs
            JOIN RouteEntity r ON r.id = rs.routeId AND r.companyId = rs.companyId
            WHERE rs.childId = c.id
            AND rs.companyId = c.companyId
            AND r.date = :date
            AND r.status IN ('PLANNED', 'IN_PROGRESS')
            AND rs.isCancelled = false
        )
    """)
    fun countChildrenWithoutRoutes(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate
    ): Int

    @Query("""
        SELECT COUNT(DISTINCT c.id)
        FROM ChildEntity c
        JOIN ScheduleEntity s ON s.childId = c.id AND s.companyId = c.companyId
        WHERE c.companyId = :companyId
        AND c.status = 'ACTIVE'
        AND s.active = true
    """)
    fun countTotalActiveChildren(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate
    ): Int

    @Query("""
        SELECT COUNT(DISTINCT d.id)
        FROM DriverEntity d
        WHERE d.companyId = :companyId
        AND d.status = 'ACTIVE'
        AND EXISTS (
            SELECT 1
            FROM RouteEntity r
            WHERE r.driverId = d.id
            AND r.companyId = d.companyId
            AND r.date = :date
            AND r.status IN ('PLANNED', 'IN_PROGRESS')
        )
        AND (
            d.licenseValidUntil < :thirtyDaysFromDate
            OR d.medicalCertificateValidUntil < :thirtyDaysFromDate
        )
    """)
    fun countDriversWithExpiringDocs(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate,
        @Param("thirtyDaysFromDate") thirtyDaysFromDate: LocalDate = date.plusDays(30)
    ): Int

    @Query("""
        SELECT COUNT(DISTINCT v.id)
        FROM VehicleEntity v
        WHERE v.companyId = :companyId
        AND v.status IN ('AVAILABLE', 'IN_ROUTE')
        AND EXISTS (
            SELECT 1
            FROM RouteEntity r
            WHERE r.vehicleId = v.id
            AND r.companyId = v.companyId
            AND r.date = :date
            AND r.status IN ('PLANNED', 'IN_PROGRESS')
        )
        AND (
            (v.insuranceValidUntil IS NOT NULL AND v.insuranceValidUntil < :thirtyDaysFromDate)
            OR (v.technicalInspectionValidUntil IS NOT NULL AND v.technicalInspectionValidUntil < :thirtyDaysFromDate)
        )
    """)
    fun countVehiclesWithExpiringDocs(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate,
        @Param("thirtyDaysFromDate") thirtyDaysFromDate: LocalDate = date.plusDays(30)
    ): Int

    @Query("""
        SELECT COUNT(DISTINCT r.driverId)
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date = :date
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND r.driverId IS NOT NULL
    """)
    fun countUniqueDrivers(
        @Param("companyId") companyId: String,
        @Param("date") date: LocalDate
    ): Int

    // ============================================
    // ALERTS QUERIES
    // ============================================

// Poprawione zapytanie w DashboardJpaRepository:

    @Query("""
    SELECT new pl.sienkiewiczmaciej.routecrm.dashboard.domain.ChildAlertItem(
        c.id,
        c.firstName,
        c.lastName,
        :queryDate
    )
    FROM ChildEntity c
    JOIN ScheduleEntity s ON s.childId = c.id AND s.companyId = c.companyId
    WHERE c.companyId = :companyId
    AND c.status = 'ACTIVE'
    AND s.active = true
    AND NOT EXISTS (
        SELECT 1
        FROM RouteStopEntity rs
        JOIN RouteEntity r ON r.id = rs.routeId AND r.companyId = rs.companyId
        WHERE rs.childId = c.id
        AND rs.companyId = c.companyId
        AND r.date = :queryDate
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND rs.isCancelled = false
    )
    AND NOT EXISTS (
        SELECT 1
        FROM ChildAbsenceEntity ca
        WHERE ca.childId = c.id
        AND ca.companyId = c.companyId
        AND ca.startDate <= :queryDate
        AND ca.endDate >= :queryDate
        AND ca.status IN ('PLANNED', 'ACTIVE')
    )
    ORDER BY c.lastName, c.firstName
""")
    fun findChildrenWithoutRoutesSingleDate(
        @Param("companyId") companyId: String,
        @Param("queryDate") queryDate: LocalDate
    ): List<ChildAlertItem>

    @Query("""
        SELECT new pl.sienkiewiczmaciej.routecrm.dashboard.domain.RouteAlertItem(
            r.id,
            r.routeName,
            r.date
        )
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date BETWEEN :startDate AND :endDate
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND r.driverId IS NULL
        ORDER BY r.date, r.routeName
    """)
    fun findRoutesWithoutDrivers(
        @Param("companyId") companyId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<RouteAlertItem>

    @Query("""
        SELECT new pl.sienkiewiczmaciej.routecrm.dashboard.domain.DriverDocAlertItem(
            d.id,
            d.firstName,
            d.lastName,
            CASE 
                WHEN d.licenseValidUntil < d.medicalCertificateValidUntil THEN 'Driving License'
                ELSE 'Medical Certificate'
            END,
            CASE 
                WHEN d.licenseValidUntil < d.medicalCertificateValidUntil THEN d.licenseValidUntil
                ELSE d.medicalCertificateValidUntil
            END
        )
        FROM DriverEntity d
        WHERE d.companyId = :companyId
        AND d.status = 'ACTIVE'
        AND (
            d.licenseValidUntil < :endDate
            OR d.medicalCertificateValidUntil < :endDate
        )
        ORDER BY 
            CASE 
                WHEN d.licenseValidUntil < d.medicalCertificateValidUntil THEN d.licenseValidUntil
                ELSE d.medicalCertificateValidUntil
            END
    """)
    fun findDriversWithExpiringDocs(
        @Param("companyId") companyId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DriverDocAlertItem>

    @Query("""
        SELECT new pl.sienkiewiczmaciej.routecrm.dashboard.domain.VehicleDocAlertItem(
            v.id,
            v.registrationNumber,
            CASE 
                WHEN v.insuranceValidUntil IS NOT NULL AND 
                     (v.technicalInspectionValidUntil IS NULL OR v.insuranceValidUntil < v.technicalInspectionValidUntil) 
                THEN 'Insurance'
                ELSE 'Technical Inspection'
            END,
            CASE 
                WHEN v.insuranceValidUntil IS NOT NULL AND 
                     (v.technicalInspectionValidUntil IS NULL OR v.insuranceValidUntil < v.technicalInspectionValidUntil) 
                THEN v.insuranceValidUntil
                ELSE v.technicalInspectionValidUntil
            END
        )
        FROM VehicleEntity v
        WHERE v.companyId = :companyId
        AND v.status IN ('AVAILABLE', 'IN_ROUTE')
        AND (
            (v.insuranceValidUntil IS NOT NULL AND v.insuranceValidUntil < :endDate)
            OR (v.technicalInspectionValidUntil IS NOT NULL AND v.technicalInspectionValidUntil < :endDate)
        )
        ORDER BY 
            CASE 
                WHEN v.insuranceValidUntil IS NOT NULL AND 
                     (v.technicalInspectionValidUntil IS NULL OR v.insuranceValidUntil < v.technicalInspectionValidUntil) 
                THEN v.insuranceValidUntil
                ELSE v.technicalInspectionValidUntil
            END
    """)
    fun findVehiclesWithExpiringDocs(
        @Param("companyId") companyId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<VehicleDocAlertItem>

    @Query("""
        SELECT new pl.sienkiewiczmaciej.routecrm.dashboard.domain.RouteAlertItem(
            r.id,
            r.routeName,
            r.date
        )
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date BETWEEN :startDate AND :endDate
        AND r.status IN ('PLANNED', 'IN_PROGRESS')
        AND r.vehicleId IS NULL
        ORDER BY r.date, r.routeName
    """)
    fun findRoutesWithoutVehicles(
        @Param("companyId") companyId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<RouteAlertItem>

    // ============================================
    // TRENDS QUERIES
    // ============================================

    @Query("""
        SELECT COUNT(DISTINCT rs.childId)
        FROM RouteStopEntity rs
        JOIN RouteEntity r ON r.id = rs.routeId AND r.companyId = rs.companyId
        WHERE rs.companyId = :companyId
        AND r.date BETWEEN :weekStart AND :weekEnd
        AND r.status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED')
    """)
    fun countUniqueChildrenInWeek(
        @Param("companyId") companyId: String,
        @Param("weekStart") weekStart: LocalDate,
        @Param("weekEnd") weekEnd: LocalDate
    ): Int

    @Query("""
        SELECT COUNT(r)
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date BETWEEN :weekStart AND :weekEnd
        AND r.status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED')
    """)
    fun countRoutesInWeek(
        @Param("companyId") companyId: String,
        @Param("weekStart") weekStart: LocalDate,
        @Param("weekEnd") weekEnd: LocalDate
    ): Int

    @Query("""
        SELECT COUNT(r)
        FROM RouteEntity r
        WHERE r.companyId = :companyId
        AND r.date BETWEEN :weekStart AND :weekEnd
        AND r.status = 'CANCELLED'
    """)
    fun countCancellationsInWeek(
        @Param("companyId") companyId: String,
        @Param("weekStart") weekStart: LocalDate,
        @Param("weekEnd") weekEnd: LocalDate
    ): Int
}