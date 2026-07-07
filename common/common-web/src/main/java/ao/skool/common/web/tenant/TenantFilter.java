package ao.skool.common.web.tenant;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.tenant.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves the tenant from the X-Skool-Tenant header and binds it to the request-scoped
 * {@link TenantContext}. Unauthenticated public paths (login, health, docs) are exempt.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Skool-Tenant";

    private final RequestScopedTenantContext tenantContext;

    public TenantFilter(RequestScopedTenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            try {
                tenantContext.bind(new TenantId(UUID.fromString(header.trim())));
            } catch (IllegalArgumentException ignored) {
                // Malformed tenant header — leave context unbound; downstream will 400/401 as appropriate.
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh");
    }
}
