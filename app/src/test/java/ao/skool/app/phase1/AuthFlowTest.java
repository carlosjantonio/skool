package ao.skool.app.phase1;

import ao.skool.app.support.ApiTestBase;
import ao.skool.common.security.Roles;
import ao.skool.identity.internal.domain.User;
import ao.skool.identity.internal.persistence.UserRepository;
import ao.skool.identity.internal.web.dto.LoginRequest;
import ao.skool.identity.internal.web.dto.RefreshRequest;
import ao.skool.identity.internal.web.dto.TokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1 — the login / refresh / logout loop.
 * <p>
 * These are the only tests that create real {@code users} rows: every other test mints a
 * signed token directly, because the filter's contract is "trust a valid token". Here the
 * point is the credential check itself, so the row and its bcrypt hash have to be real.
 */
class AuthFlowTest extends ApiTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private record Credentials(User user, String email, String password) {}

    private Credentials givenUser(String password, boolean active, String... roles) {
        String email = "user" + SEQ.incrementAndGet() + "@escola.test";
        User user = new User(UUID.randomUUID(), tenant, email,
                passwordEncoder.encode(password), "Ana Silva", Set.of(roles));
        if (!active) user.deactivate();
        users.save(user);
        return new Credentials(user, email, password);
    }

    private TokenResponse login(String email, String password) throws Exception {
        return post(anonymous(), "/api/auth/login", new LoginRequest(email, password), TokenResponse.class);
    }

    @Test
    @DisplayName("valid credentials return a usable access token and the user's profile")
    void loginSucceeds() throws Exception {
        Credentials c = givenUser("propina2025", true, Roles.TEACHER);

        TokenResponse token = login(c.email(), c.password());

        assertThat(token.userId()).isEqualTo(c.user().id());
        assertThat(token.tenantId()).isEqualTo(tenant);
        assertThat(token.email()).isEqualTo(c.email());
        assertThat(token.fullName()).isEqualTo("Ana Silva");
        assertThat(token.roles()).containsExactly(Roles.TEACHER);
        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.refreshToken()).isNotBlank();
        assertThat(token.accessTokenExpiresAt()).isAfter(java.time.Instant.now());

        // The token the endpoint handed out actually opens a protected door.
        Actor asUser = new Actor(c.user().id(), "Ana Silva", c.email(), token.accessToken());
        assertThat(getStatus(asUser, "/api/subjects")).isEqualTo(200);
    }

    @Test
    @DisplayName("login is case-insensitive on the email")
    void loginIgnoresEmailCase() throws Exception {
        Credentials c = givenUser("propina2025", true, Roles.ADMIN);

        TokenResponse token = login(c.email().toUpperCase(), c.password());

        assertThat(token.userId()).isEqualTo(c.user().id());
    }

    @Test
    @DisplayName("a wrong password is rejected, and says nothing about why")
    void wrongPasswordIsRejected() throws Exception {
        Credentials c = givenUser("propina2025", true, Roles.TEACHER);

        var result = exec(anonymous(), MockMvcRequestBuilders.post("/api/auth/login"),
                new LoginRequest(c.email(), "wrong-password"));

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode problem = json.readTree(bodyOf(result));
        assertThat(problem.get("type").asText()).isEqualTo("urn:skool:error:unauthorized");
        // Same generic message as an unknown account — no user enumeration.
        assertThat(problem.get("title").asText()).isEqualTo("Sessão inválida ou expirada.");
    }

    @Test
    @DisplayName("an unknown email is rejected exactly like a wrong password")
    void unknownEmailIsRejected() throws Exception {
        assertThat(postStatus(anonymous(), "/api/auth/login",
                new LoginRequest("ninguem@escola.test", "propina2025"))).isEqualTo(401);
    }

    @Test
    @DisplayName("a deactivated user cannot log in even with the right password")
    void deactivatedUserCannotLogIn() throws Exception {
        Credentials c = givenUser("propina2025", false, Roles.TEACHER);

        assertThat(postStatus(anonymous(), "/api/auth/login",
                new LoginRequest(c.email(), c.password()))).isEqualTo(401);
    }

    @Test
    @DisplayName("refresh rotates: the used token is burned and a new pair is issued")
    void refreshRotatesTheToken() throws Exception {
        Credentials c = givenUser("propina2025", true, Roles.TEACHER);
        TokenResponse first = login(c.email(), c.password());

        TokenResponse second = post(anonymous(), "/api/auth/refresh",
                new RefreshRequest(first.refreshToken()), TokenResponse.class);

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(second.userId()).isEqualTo(c.user().id());

        // Replaying the old refresh token must fail — that's the whole point of rotation.
        assertThat(postStatus(anonymous(), "/api/auth/refresh",
                new RefreshRequest(first.refreshToken()))).isEqualTo(401);

        // The newly issued one still works.
        assertThat(postStatus(anonymous(), "/api/auth/refresh",
                new RefreshRequest(second.refreshToken()))).isEqualTo(200);
    }

    @Test
    @DisplayName("logout revokes the refresh token")
    void logoutRevokesTheRefreshToken() throws Exception {
        Credentials c = givenUser("propina2025", true, Roles.TEACHER);
        TokenResponse token = login(c.email(), c.password());

        var logout = exec(anonymous(), MockMvcRequestBuilders.post("/api/auth/logout"),
                new RefreshRequest(token.refreshToken()));
        assertThat(logout.getResponse().getStatus()).isEqualTo(204);

        assertThat(postStatus(anonymous(), "/api/auth/refresh",
                new RefreshRequest(token.refreshToken()))).isEqualTo(401);
    }

    @Test
    @DisplayName("logging out with an unknown token is a no-op, not an error")
    void logoutIsIdempotent() throws Exception {
        var result = exec(anonymous(), MockMvcRequestBuilders.post("/api/auth/logout"),
                new RefreshRequest("a-token-that-was-never-issued"));
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    @DisplayName("refresh tokens are never stored in the clear")
    void refreshTokensAreHashedAtRest() throws Exception {
        Credentials c = givenUser("propina2025", true, Roles.TEACHER);
        TokenResponse token = login(c.email(), c.password());

        // Stored value is a SHA-256 hex digest, so a database leak does not hand out sessions.
        var stored = jdbcRefreshHashes(c.user().id());
        assertThat(stored).isNotEmpty();
        assertThat(stored).noneMatch(h -> h.equals(token.refreshToken()));
        assertThat(stored).allMatch(h -> h.matches("[0-9a-f]{64}"));
    }

    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private java.util.List<String> jdbcRefreshHashes(UUID userId) {
        return jdbc.queryForList(
                "select token_hash from refresh_tokens where user_id = ?", String.class, userId);
    }
}
