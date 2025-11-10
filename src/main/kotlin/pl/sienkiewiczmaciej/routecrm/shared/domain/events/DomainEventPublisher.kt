// shared/domain/events/DomainEventPublisher.kt
package pl.sienkiewiczmaciej.routecrm.shared.domain.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Publisher for domain events.
 * Events are published asynchronously to listeners.
 */
interface DomainEventPublisher {
    fun publish(event: DomainEvent)
    fun publishAll(events: List<DomainEvent>)
}

@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}