// shared/domain/events/DomainEvent.kt
package pl.sienkiewiczmaciej.routecrm.shared.domain.events

import java.time.Instant

/**
 * Base interface for all domain events in the system.
 * Domain events represent something that has happened in the domain.
 */
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val aggregateId: String
    val aggregateType: String
}