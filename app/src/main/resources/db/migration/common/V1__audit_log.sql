-- Cross-cutting append-only audit log. Every module writes to this single table
-- via the AuditLog interceptor. No UPDATE/DELETE grants — enforced by policy, not
-- schema, since we still need the app role to insert.

CREATE TABLE audit_log (
    id            UUID        PRIMARY KEY,
    tenant_id     UUID        NOT NULL,
    actor_id      VARCHAR(64) NOT NULL,
    action        VARCHAR(64) NOT NULL,
    target_type   VARCHAR(64),
    target_id     VARCHAR(64),
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_tenant_time ON audit_log (tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_log_actor_time  ON audit_log (actor_id,  occurred_at DESC);
CREATE INDEX idx_audit_log_target      ON audit_log (target_type, target_id);
