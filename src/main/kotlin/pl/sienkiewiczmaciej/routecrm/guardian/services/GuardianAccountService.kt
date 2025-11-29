// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/guardian/services/GuardianAccountService.kt
package pl.sienkiewiczmaciej.routecrm.guardian.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.auth.global.AccountStatus
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianAuditLogJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.temporal.ChronoUnit

data class GuardianAccountInfo(
    val hasAccount: Boolean,
    val lastLogin: Instant?,
    val loginCount30Days: Int,
    val loginCount7Days: Int,
    val accountCreatedAt: Instant?,
    val accountStatus: AccountStatus
)

@Service
class GuardianAccountService(
    private val globalGuardianRepository: GlobalGuardianRepository,
    private val auditLogRepository: GuardianAuditLogJpaRepository
) {
    @Transactional(readOnly = true)
    suspend fun getAccountInfo(
        companyId: CompanyId,
        guardianId: GuardianId,
        guardian: Guardian
    ): GuardianAccountInfo {
        if (guardian.email == null) {
            return GuardianAccountInfo(
                hasAccount = false,
                lastLogin = null,
                loginCount30Days = 0,
                loginCount7Days = 0,
                accountCreatedAt = null,
                accountStatus = AccountStatus.INACTIVE
            )
        }

        val globalGuardian = globalGuardianRepository.findByEmail(guardian.email)
            ?: return GuardianAccountInfo(
                hasAccount = false,
                lastLogin = null,
                loginCount30Days = 0,
                loginCount7Days = 0,
                accountCreatedAt = null,
                accountStatus = AccountStatus.INACTIVE
            )

        val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)

        val loginCount30Days = withContext(Dispatchers.IO) {
            auditLogRepository.countLoginsByGuardianAndCompanySince(
                globalGuardianId = globalGuardian.id.value,
                companyId = companyId.value,
                since = thirtyDaysAgo
            )
        }

        val loginCount7Days = withContext(Dispatchers.IO) {
            auditLogRepository.countLoginsByGuardianAndCompanySince(
                globalGuardianId = globalGuardian.id.value,
                companyId = companyId.value,
                since = sevenDaysAgo
            )
        }

        val lastLogin = withContext(Dispatchers.IO) {
            auditLogRepository.findLastLoginByGuardianAndCompany(
                globalGuardianId = globalGuardian.id.value,
                companyId = companyId.value
            )
        }

        val accountStatus = when {
            globalGuardian.accountLocked -> AccountStatus.LOCKED
            !globalGuardian.emailVerified -> AccountStatus.PENDING_VERIFICATION
            else -> AccountStatus.ACTIVE
        }

        return GuardianAccountInfo(
            hasAccount = true,
            lastLogin = lastLogin,
            loginCount30Days = loginCount30Days,
            loginCount7Days = loginCount7Days,
            accountCreatedAt = globalGuardian.createdAt,
            accountStatus = accountStatus
        )
    }
}