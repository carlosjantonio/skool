package ao.skool.common.security.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit event. Backed by the {@code audit_log} table from V1 in common
 * migrations. Never updated — mutations should insert a new row, not modify an old
 * one, to preserve the audit trail for Lei 22/11 compliance.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", length = 64)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    protected AuditLogEntry() {}

    public AuditLogEntry(UUID id, UUID tenantId, String actorId, String action,
                         String targetType, String targetId) {
        this.id = id;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String actorId() { return actorId; }
    public String action() { return action; }
    public String targetType() { return targetType; }
    public String targetId() { return targetId; }
    public Instant occurredAt() { return occurredAt; }
}
