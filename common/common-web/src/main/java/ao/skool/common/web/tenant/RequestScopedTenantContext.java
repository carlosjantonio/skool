package ao.skool.common.web.tenant;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class RequestScopedTenantContext implements TenantContext {

    private TenantId explicit;

    void bind(TenantId tenantId) {
        this.explicit = tenantId;
    }

    @Override
    public TenantId current() {
        TenantId resolved = resolve();
        if (resolved == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.tenant.missing");
        }
        return resolved;
    }

    @Override
    public boolean isPresent() {
        return resolve() != null;
    }

    private TenantId resolve() {
        if (explicit != null) return explicit;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            return p.tenantId();
        }
        return null;
    }
}
