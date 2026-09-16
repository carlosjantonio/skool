package ao.skool.app.support;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.TenantId;
import ao.skool.common.security.jwt.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Base for the API-level tests. Everything is driven through MockMvc rather than by
 * calling services directly, because the parts most likely to break are the seams:
 * the JWT filter, {@code @PreAuthorize} role gates, tenant resolution, JSON contracts
 * and the RFC 7807 error shape. Calling a service in isolation skips all of them.
 * <p>
 * <b>Isolation strategy:</b> tests are not {@code @Transactional}. Rolling back would
 * hide exactly the behaviour several of these tests exist to pin down — the
 * {@code REQUIRES_NEW} audit writer, the {@code REQUIRES_NEW} attempt insert, and the
 * unique-constraint handling that only fires on flush. Instead, every test method gets
 * a <b>fresh tenant UUID</b>. Every query in the system is tenant-scoped, so tests
 * cannot see each other's rows even though they share one H2 database and one
 * application context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RecordedEvents.class)
public abstract class ApiTestBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JwtService jwtService;
    @Autowired protected RecordedEvents events;

    /** Fresh per test method — see the isolation note on the class. */
    protected UUID tenant;

    @BeforeEach
    void resetPerTestState() {
        tenant = UUID.randomUUID();
        events.clear();
    }

    // ---------------------------------------------------------------- actors

    /**
     * A caller identified by a real, signed JWT. The user need not exist in the
     * {@code users} table: the filter trusts the token, which is the production
     * contract. Tests that specifically exercise login create real rows instead.
     */
    public record Actor(UUID userId, String fullName, String email, String bearer) {}

    protected Actor actor(String fullName, String... roles) {
        UUID userId = UUID.randomUUID();
        return actor(userId, fullName, roles);
    }

    protected Actor actor(UUID userId, String fullName, String... roles) {
        String email = fullName.toLowerCase().replace(' ', '.') + "@escola.test";
        var principal = new SkoolPrincipal(userId.toString(), TenantId.of(tenant), email, fullName, Set.of(roles));
        return new Actor(userId, fullName, email, jwtService.issueAccessToken(principal).value());
    }

    /** An actor belonging to a *different* school — for tenant-isolation assertions. */
    protected Actor actorInOtherTenant(String fullName, String... roles) {
        UUID userId = UUID.randomUUID();
        var principal = new SkoolPrincipal(userId.toString(), TenantId.of(UUID.randomUUID()),
                "outsider@outra-escola.test", fullName, Set.of(roles));
        return new Actor(userId, fullName, "outsider@outra-escola.test",
                jwtService.issueAccessToken(principal).value());
    }

    protected Actor admin() { return actor("Admin Demo", "ADMIN"); }

    protected Actor director() { return actor("Directora Demo", "DIRECTOR"); }

    protected Actor secretary() { return actor("Secretaria Demo", "SECRETARY"); }

    protected Actor teacher() { return actor("Ana Silva", "TEACHER"); }

    protected Actor student(UUID userId) { return actor(userId, "Joao Baptista", "STUDENT"); }

    protected Actor guardian(UUID userId) { return actor(userId, "Maria Baptista", "GUARDIAN"); }

    /** No Authorization header at all. */
    protected Actor anonymous() { return new Actor(null, "anonymous", null, null); }

    // ------------------------------------------------------- direct API calls

    /**
     * Runs {@code work} as if it were inside an HTTP request from {@code as}.
     * <p>
     * Needed when a test calls a module's public {@code api} interface directly rather
     * than over MockMvc: {@link ao.skool.common.web.tenant.RequestScopedTenantContext}
     * is {@code @RequestScope} and reads the tenant off the security context, so both
     * have to be bound or the call fails with {@code error.tenant.missing}.
     */
    protected <T> T inRequestScope(Actor as, java.util.concurrent.Callable<T> work) throws Exception {
        SkoolPrincipal principal = jwtService.parse(as.bearer());
        var attributes = new org.springframework.web.context.request.ServletRequestAttributes(
                new org.springframework.mock.web.MockHttpServletRequest());
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(attributes);
        var authorities = principal.roles().stream()
                .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, authorities));
        try {
            return work.call();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
            attributes.requestCompleted();
        }
    }

    // -------------------------------------------------------------- requests

    protected <T> T post(Actor as, String path, Object payload, Class<T> type) throws Exception {
        return read(ok(as, MockMvcRequestBuilders.post(path), payload), type);
    }

    protected <T> T post(Actor as, String path, Object payload, TypeReference<T> type) throws Exception {
        return read(ok(as, MockMvcRequestBuilders.post(path), payload), type);
    }

    protected <T> T put(Actor as, String path, Object payload, Class<T> type) throws Exception {
        return read(ok(as, MockMvcRequestBuilders.put(path), payload), type);
    }

    protected <T> T patch(Actor as, String path, Object payload, Class<T> type) throws Exception {
        return read(ok(as, MockMvcRequestBuilders.patch(path), payload), type);
    }

    protected <T> T get(Actor as, String path, Class<T> type) throws Exception {
        return read(ok(as, MockMvcRequestBuilders.get(path), null), type);
    }

    protected <T> T get(Actor as, String path, TypeReference<T> type) throws Exception {
        return read(ok(as, MockMvcRequestBuilders.get(path), null), type);
    }

    /** Status code of a call that is expected to fail (or whose status is the assertion). */
    protected int postStatus(Actor as, String path, Object payload) throws Exception {
        return exec(as, MockMvcRequestBuilders.post(path), payload).getResponse().getStatus();
    }

    protected int putStatus(Actor as, String path, Object payload) throws Exception {
        return exec(as, MockMvcRequestBuilders.put(path), payload).getResponse().getStatus();
    }

    protected int patchStatus(Actor as, String path, Object payload) throws Exception {
        return exec(as, MockMvcRequestBuilders.patch(path), payload).getResponse().getStatus();
    }

    protected int getStatus(Actor as, String path) throws Exception {
        return exec(as, MockMvcRequestBuilders.get(path), null).getResponse().getStatus();
    }

    protected int deleteStatus(Actor as, String path) throws Exception {
        return exec(as, MockMvcRequestBuilders.delete(path), null).getResponse().getStatus();
    }

    /** Full result, for tests that need headers or raw bytes (e.g. the boletim PDF). */
    protected MvcResult exec(Actor as, MockHttpServletRequestBuilder rb, Object payload) throws Exception {
        if (as != null && as.bearer() != null) {
            rb.header("Authorization", "Bearer " + as.bearer());
        }
        if (payload != null) {
            rb.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload));
        }
        rb.accept(MediaType.ALL);
        return mvc.perform(rb).andReturn();
    }

    private MvcResult ok(Actor as, MockHttpServletRequestBuilder rb, Object payload) throws Exception {
        MvcResult result = exec(as, rb, payload);
        int status = result.getResponse().getStatus();
        if (status < 200 || status >= 300) {
            throw new AssertionError("Expected 2xx but got " + status + " — body: " + bodyOf(result));
        }
        return result;
    }

    protected String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private <T> T read(MvcResult result, Class<T> type) throws Exception {
        return json.readValue(bodyOf(result), type);
    }

    private <T> T read(MvcResult result, TypeReference<T> type) throws Exception {
        return json.readValue(bodyOf(result), type);
    }
}
