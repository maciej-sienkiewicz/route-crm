package pl.sienkiewiczmaciej.routecrm.route.domain.services

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteId
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStop
import pl.sienkiewiczmaciej.routecrm.route.domain.RouteStopRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

enum class InsertionStrategy {
    GAP_BASED,
    REBALANCE_RETRY,
    PESSIMISTIC_LOCK
}

data class InsertionResult(
    val insertedStops: List<RouteStop>,
    val strategyUsed: InsertionStrategy
)

@Component
class StopInsertionService(
    private val calculator: GapBasedStopOrderCalculator,
    private val stopRepository: RouteStopRepository,
    private val entityManager: EntityManager,
) {

    suspend fun insertStops(
        companyId: CompanyId,
        routeId: RouteId,
        stopsToInsert: List<RouteStop>,
        afterOrder: Int?
    ): InsertionResult {
        return try {
            tryGapBasedInsertion(companyId, routeId, stopsToInsert, afterOrder)
        } catch (e: InsufficientGapException) {
            try {
                tryRebalanceAndRetry(companyId, routeId, stopsToInsert, afterOrder)
            } catch (ex: Exception) {
                tryPessimisticLockInsertion(companyId, routeId, stopsToInsert, afterOrder)
            }
        }
    }

    private suspend fun tryGapBasedInsertion(
        companyId: CompanyId,
        routeId: RouteId,
        stopsToInsert: List<RouteStop>,
        afterOrder: Int?
    ): InsertionResult {
        val existingStops = stopRepository.findByRoute(companyId, routeId, includeCancelled = false)

        val result = calculator.calculateOrdersForMultipleInsertions(
            existingStops = existingStops,
            afterOrder = afterOrder,
            count = stopsToInsert.size
        )

        val stopsWithNewOrders = stopsToInsert.mapIndexed { index, stop ->
            stop.updateOrder(result.orders[index])
        }

        val savedStops = stopRepository.saveAll(stopsWithNewOrders)

        return InsertionResult(
            insertedStops = savedStops,
            strategyUsed = InsertionStrategy.GAP_BASED
        )
    }

    private suspend fun tryRebalanceAndRetry(
        companyId: CompanyId,
        routeId: RouteId,
        stopsToInsert: List<RouteStop>,
        afterOrder: Int?
    ): InsertionResult {
        val existingStops = stopRepository.findByRoute(companyId, routeId, includeCancelled = false)
        val rebalancedStops = calculator.rebalance(existingStops)
        stopRepository.saveAll(rebalancedStops)

        val refreshedStops = stopRepository.findByRoute(companyId, routeId, includeCancelled = false)
        val result = calculator.calculateOrdersForMultipleInsertions(
            existingStops = refreshedStops,
            afterOrder = afterOrder,
            count = stopsToInsert.size
        )

        val stopsWithNewOrders = stopsToInsert.mapIndexed { index, stop ->
            stop.updateOrder(result.orders[index])
        }

        val savedStops = stopRepository.saveAll(stopsWithNewOrders)

        return InsertionResult(
            insertedStops = savedStops,
            strategyUsed = InsertionStrategy.REBALANCE_RETRY
        )
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    private suspend fun tryPessimisticLockInsertion(
        companyId: CompanyId,
        routeId: RouteId,
        stopsToInsert: List<RouteStop>,
        afterOrder: Int?
    ): InsertionResult {
        val existingStops = stopRepository.findByRoute(companyId, routeId, includeCancelled = false)

        val pickupPosition = afterOrder ?: 1
        val stopsToShift = existingStops.filter { it.stopOrder >= pickupPosition }

        val tempShifted = stopsToShift.map { stop ->
            stop.updateOrder(-(stop.stopOrder + 10000))
        }
        stopRepository.saveAll(tempShifted)
        entityManager.flush()

        val stopsWithOrders = stopsToInsert.mapIndexed { index, stop ->
            stop.updateOrder(pickupPosition + index)
        }
        val savedNewStops = stopRepository.saveAll(stopsWithOrders)
        entityManager.flush()

        val refreshedStops = stopRepository.findByRoute(companyId, routeId, includeCancelled = false)
        val negativeStops = refreshedStops.filter { it.stopOrder < 0 }

        val finalShifted = negativeStops.map { stop ->
            stop.updateOrder((-stop.stopOrder) + stopsToInsert.size)
        }
        stopRepository.saveAll(finalShifted)
        entityManager.flush()

        return InsertionResult(
            insertedStops = savedNewStops,
            strategyUsed = InsertionStrategy.PESSIMISTIC_LOCK
        )
    }
}