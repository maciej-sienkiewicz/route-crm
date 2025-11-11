// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/absence/infrastructure/services/AbsenceApplicationService.kt
package pl.sienkiewiczmaciej.routecrm.absence.infrastructure.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsence
import pl.sienkiewiczmaciej.routecrm.absence.domain.ChildAbsenceId
import pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopJpaRepository

@Service
class AbsenceApplicationService(
    private val routeStopJpaRepository: RouteStopJpaRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    suspend fun applyAbsenceToRouteStops(absence: ChildAbsence): Int {
        logger.info("Applying absence ${absence.id.value} to route stops")

        val affectedStops = findAffectedRouteStops(absence)

        if (affectedStops.isEmpty()) {
            logger.info("No route stops found to apply absence ${absence.id.value}")
            return 0
        }

        val cancelledStops = affectedStops.map { stop ->
            stop.cancel("Absence: ${absence.reason ?: "No reason provided"}")
        }

        withContext(Dispatchers.IO) {
            cancelledStops.forEach { stop ->
                val entity = pl.sienkiewiczmaciej.routecrm.route.infrastructure.RouteStopEntity.fromDomain(stop)
                val updatedEntity = entity.copy(
                    cancelledByAbsenceId = absence.id.value
                )
                routeStopJpaRepository.save(updatedEntity)
            }
        }

        logger.info("Applied absence ${absence.id.value} to ${cancelledStops.size} route stops")
        return cancelledStops.size
    }

    @Transactional
    suspend fun removeAbsenceFromRouteStops(absenceId: ChildAbsenceId): List<String> {
        logger.info("Finding route stops affected by cancelled absence ${absenceId.value}")

        val affectedStopIds = withContext(Dispatchers.IO) {
            routeStopJpaRepository.findByCancelledByAbsenceId(absenceId.value)
                .map { it.id }
        }

        logger.info("Found ${affectedStopIds.size} route stops that were cancelled by absence ${absenceId.value}")
        logger.warn("Manual intervention required: Operator must decide whether to restore these stops")

        return affectedStopIds
    }

    private suspend fun findAffectedRouteStops(absence: ChildAbsence): List<pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop> {
        return withContext(Dispatchers.IO) {
            val allStopsForChild = routeStopJpaRepository.findByChildIdInDateRange(
                absence.companyId.value,
                absence.childId.value,
                absence.startDate,
                absence.endDate
            )

            allStopsForChild
                .filter { !it.isCancelled }
                .filter { stopEntity ->
                    val stopDate = stopEntity.routeDate
                    val matchesDateRange = !stopDate.isBefore(absence.startDate) &&
                            !stopDate.isAfter(absence.endDate)

                    if (!matchesDateRange) return@filter false

                    when (absence.absenceType) {
                        pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType.FULL_DAY -> true
                        pl.sienkiewiczmaciej.routecrm.absence.domain.AbsenceType.SPECIFIC_SCHEDULE ->
                            stopEntity.scheduleId == absence.scheduleId?.value
                    }
                }
                .map { it.toDomain() }
        }
    }
}