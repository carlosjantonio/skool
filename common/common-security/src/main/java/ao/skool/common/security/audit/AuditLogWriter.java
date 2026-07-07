package ao.skool.common.security.audit;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.TenantId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes to the {@code audit_log} table from any module. Callers pass the action name
 * plus optional target metadata; the actor and tenant are derived from the current
 * security context.
 * <p>
 * Runs in {@code REQUIRES_NEW} so the audit row commits even if the surrounding
 * business transaction rolls back — you want a record of "attempted grade change"
 * even when the change itself fails validation later.
 */
@Service
public class AuditLogWriter {

    private final AuditLogRepository repo;

    public AuditLogWriter(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, String targetId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actorId = "system";
        TenantId tenantId = null;
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            actorId = p.userId();
            tenantId = p.tenantId();
        }
        if (tenantId == null) return; // no tenant means no valid session — skip rather than fail
        repo.save(new AuditLogEntry(UUID.randomUUID(), tenantId.value(), actorId, action, targetType, targetId));
    }
}
