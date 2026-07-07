# ADR-0002: In-process event bus first, Kafka later

- **Status:** Accepted
- **Date:** 2026-07-07
- **Deciders:** Carlos (solo build)

## Context

Cross-module side effects — "student marked absent → guardian gets SMS", "grade posted → notification sent", "invoice overdue → guardian alerted" — need a decoupled seam so the emitting module never learns about the consuming module. The prompt specifies Kafka (or RabbitMQ) as the async transport.

Running Kafka in dev and prod on day one adds operational weight (broker + Zookeeper/KRaft, topic management, schema evolution, consumer offsets, DLQs) before any real event flows exist.

## Decision

For Phase 0–6, publish domain events through **Spring's `ApplicationEventPublisher`** and consume them with `@TransactionalEventListener(AFTER_COMMIT)`. Every event implements the shared `DomainEvent` interface (in `common-domain`) with `eventId`, `tenantId`, and `occurredAt`.

When we split a module to its own deployable — or when a new consumer needs to run on a different node — swap the transport by:

1. Adding a Kafka publisher that also receives the `ApplicationEventPublisher` event and forwards it to a topic named `skool.<module>.<event>`.
2. Adding a `@KafkaListener` on the consuming side that re-publishes the event to the local `ApplicationEventPublisher`.

Because every event is already a `DomainEvent` with a stable serialization contract, the consumer code doesn't change.

## Consequences

**Positive**
- Zero infra beyond Postgres in Phase 0.
- Events are transactional: `AFTER_COMMIT` guarantees a consumer only sees the event if the producing transaction actually committed.
- The Kafka swap is a transport concern, not a redesign — event shape and handler code stay the same.

**Negative**
- Events don't survive a crash between commit and delivery — for critical flows (invoice payment webhook → billing), we'll add an outbox table before the crash-safety guarantee matters.
- Consumers run in the producer's JVM — a slow consumer slows the request thread unless we mark listeners `@Async`.

**Trigger to migrate to Kafka**
- We extract any module to its own deployable (need cross-process events).
- Any consumer must survive a producer restart (outbox alone isn't enough).
- We start replaying events for a new read model.
