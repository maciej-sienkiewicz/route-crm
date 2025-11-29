package pl.sienkiewiczmaciej.routecrm.guardian.services

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.guardian.contact.ContactHistoryRepository
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GuardianStats(
    val totalChildren: Int,
    val activeChildren: Int,
    val upcomingRoutes: Int,
    val recentContacts: Int
)

@Service
class GuardianStatsService(
    private val contactHistoryRepository: ContactHistoryRepository,
    private val routeJpaRepository: RouteJpaRepository
) {
    @Transactional(readOnly = true)
    suspend fun calculateStats(
        companyId: CompanyId,
        guardianId: GuardianId,
        children: List<GuardianChild>
    ): GuardianStats = coroutineScope {
        val upcomingRoutesDeferred = async {
            countUpcomingRoutes(companyId, children)
        }

        val recentContactsDeferred = async {
            countRecentContacts(companyId, guardianId)
        }

        GuardianStats(
            totalChildren = children.size,
            activeChildren = children.count { it.isActive() },
            upcomingRoutes = upcomingRoutesDeferred.await(),
            recentContacts = recentContactsDeferred.await()
        )
    }

    private suspend fun countUpcomingRoutes(
        companyId: CompanyId,
        children: List<GuardianChild>
    ): Int {
        if (children.isEmpty()) return 0

        val childIds = children.map { it.child.id.value }
        val today = LocalDate.now()

        val count = 0

        return count
    }

    private suspend fun countRecentContacts(
        companyId: CompanyId,
        guardianId: GuardianId
    ): Int {
        val thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS)
        val contacts = contactHistoryRepository.findByGuardian(companyId, guardianId)
        return contacts.count { it.contactedAt.isAfter(thirtyDaysAgo) }
    }
}