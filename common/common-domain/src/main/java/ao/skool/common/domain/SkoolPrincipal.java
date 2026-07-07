package ao.skool.common.domain;

import java.util.Set;

/**
 * The authenticated user for the current request. Populated from a valid JWT by the
 * security layer, then available via {@code SecurityContextHolder}. Kept in
 * common-domain so both web (tenant resolution) and security (filter) can reference it
 * without creating a cycle between the two.
 */
public record SkoolPrincipal(
        String userId,
        TenantId tenantId,
        String email,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
