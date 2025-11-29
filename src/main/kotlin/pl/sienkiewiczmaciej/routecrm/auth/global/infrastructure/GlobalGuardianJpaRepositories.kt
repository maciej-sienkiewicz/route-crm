package pl.sienkiewiczmaciej.routecrm.auth.global.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GlobalGuardianJpaRepository : JpaRepository<GlobalGuardianEntity, String> {
    fun findByEmail(email: String): GlobalGuardianEntity?
    fun existsByEmail(email: String): Boolean
}

interface EmailVerificationJpaRepository : JpaRepository<EmailVerificationEntity, String> {
    fun findByToken(token: String): EmailVerificationEntity?

    @Query("""
        SELECT e FROM EmailVerificationEntity e
        WHERE e.globalGuardianId = :guardianId
        AND e.verifiedAt IS NULL
        AND e.expiresAt > CURRENT_TIMESTAMP
        ORDER BY e.createdAt DESC
        LIMIT 1
    """)
    fun findActiveByGuardian(guardianId: String): EmailVerificationEntity?

    fun deleteByGlobalGuardianId(guardianId: String)
}

interface PasswordResetTokenJpaRepository : JpaRepository<PasswordResetTokenEntity, String> {
    fun findByToken(token: String): PasswordResetTokenEntity?

    @Query("""
        SELECT p FROM PasswordResetTokenEntity p
        WHERE p.globalGuardianId = :guardianId
        AND p.usedAt IS NULL
        AND p.expiresAt > CURRENT_TIMESTAMP
        ORDER BY p.createdAt DESC
        LIMIT 1
    """)
    fun findActiveByGuardian(guardianId: String): PasswordResetTokenEntity?

    fun deleteByGlobalGuardianId(guardianId: String)
}
