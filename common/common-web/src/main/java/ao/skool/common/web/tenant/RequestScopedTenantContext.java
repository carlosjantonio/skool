package ao.skool.common.web.tenant;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class RequestScopedTenantContext implements TenantContext {

    private TenantId tenantId;

    void bind(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public TenantId current() {
        if (tenantId == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.tenant.missing");
        }
        return tenantId;
    }

    @Override
    public boolean isPresent() {
        return tenantId != null;
    }
}
