package ao.skool.common.security.audit;

import ao.skool.common.domain.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit event. Persisted append-only by the common audit table (V1__audit_log.sql).
 * The concrete writer lives in the app module so it can reuse the shared DataSource.
 */
public record AuditLogEntry(
        UUID id,
        TenantId tenantId,
        String actorId,
        String action,
        String targetType,
        String targetId,
        Instant occurredAt
) {}
