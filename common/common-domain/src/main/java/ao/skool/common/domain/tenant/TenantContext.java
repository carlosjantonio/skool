package ao.skool.common.domain.tenant;

import ao.skool.common.domain.TenantId;

/**
 * Ambient tenant for the current request/thread. Set by the tenant filter at the web edge,
 * read by repositories and services. Deliberately request-scoped in common-web.
 */
public interface TenantContext {

    TenantId current();

    boolean isPresent();
}
