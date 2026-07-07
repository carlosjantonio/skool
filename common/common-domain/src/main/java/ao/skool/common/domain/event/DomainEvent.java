package ao.skool.common.domain.event;

import ao.skool.common.domain.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * Base for all cross-module domain events. Published via Spring's ApplicationEventPublisher
 * in Phase 0; the same interface will be serialized to Kafka after the microservice split.
 */
public interface DomainEvent {

    UUID eventId();

    TenantId tenantId();

    Instant occurredAt();
}
