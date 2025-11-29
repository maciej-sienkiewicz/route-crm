package pl.sienkiewiczmaciej.routecrm.guardian.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface GuardianAuditLogJpaRepository : JpaRepository<GuardianAuditLogEntity, Long> {

    @Query("""
        SELECT COUNT(*)
        FROM GuardianAuditLogEntity g
        WHERE g.globalGuardianId = :globalGuardianId
          AND g.companyId = :companyId
          AND g.eventType = 'LOGIN'
          AND g.createdAt >= :since
    """)
    fun countLoginsByGuardianAndCompanySince(
        @Param("globalGuardianId") globalGuardianId: String,
        @Param("companyId") companyId: String,
        @Param("since") since: Instant
    ): Int

    @Query("""
        SELECT g.createdAt
        FROM GuardianAuditLogEntity g
        WHERE g.globalGuardianId = :globalGuardianId
          AND g.companyId = :companyId
          AND g.eventType = 'LOGIN'
        ORDER BY g.createdAt DESC
        LIMIT 1
    """)
    fun findLastLoginByGuardianAndCompany(
        @Param("globalGuardianId") globalGuardianId: String,
        @Param("companyId") companyId: String
    ): Instant?
}
