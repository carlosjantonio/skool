package ao.skool.app.phase1;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.TenantId;
import ao.skool.common.security.JwtProperties;
import ao.skool.common.security.jwt.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 1 — token issuing and parsing, without Spring. These are the claims every
 * downstream module trusts: get them wrong and tenancy, authorship and RBAC all break
 * at once.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-at-least-32-bytes-long-abcdef";

    private JwtService serviceWith(String secret, String issuer) {
        return new JwtService(new JwtProperties(issuer, secret, Duration.ofMinutes(30), Duration.ofDays(30)));
    }

    private SkoolPrincipal principal() {
        return new SkoolPrincipal(
                UUID.randomUUID().toString(),
                TenantId.of(UUID.randomUUID()),
                "ana@escola.ao",
                "Ana Silva",
                Set.of("TEACHER", "DIRECTOR"));
    }

    @Test
    @DisplayName("a token round-trips every claim the modules depend on")
    void roundTripsAllClaims() {
        JwtService jwt = serviceWith(SECRET, "skool");
        SkoolPrincipal original = principal();

        var issued = jwt.issueAccessToken(original);
        SkoolPrincipal parsed = jwt.parse(issued.value());

        assertThat(parsed.userId()).isEqualTo(original.userId());
        assertThat(parsed.tenantId()).isEqualTo(original.tenantId());
        assertThat(parsed.email()).isEqualTo(original.email());
        // fullName rides in the token specifically so the forum can attribute a post
        // without a synchronous call into identity.
        assertThat(parsed.fullName()).isEqualTo("Ana Silva");
        assertThat(parsed.roles()).containsExactlyInAnyOrder("TEACHER", "DIRECTOR");
        assertThat(parsed.hasRole("TEACHER")).isTrue();
        assertThat(parsed.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("expiry is stamped from the configured TTL")
    void setsExpiryFromTtl() {
        JwtService jwt = serviceWith(SECRET, "skool");
        var issued = jwt.issueAccessToken(principal());
        assertThat(issued.expiresAt()).isBetween(
                java.time.Instant.now().plus(Duration.ofMinutes(29)),
                java.time.Instant.now().plus(Duration.ofMinutes(31)));
    }

    @Test
    @DisplayName("a token signed with another key is rejected")
    void rejectsForeignSignature() {
        JwtService ours = serviceWith(SECRET, "skool");
        JwtService attacker = serviceWith("a-completely-different-secret-32-bytes-xx", "skool");

        String forged = attacker.issueAccessToken(principal()).value();

        assertThatThrownBy(() -> ours.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a token from another issuer is rejected")
    void rejectsForeignIssuer() {
        JwtService ours = serviceWith(SECRET, "skool");
        JwtService other = serviceWith(SECRET, "someone-else");

        String foreign = other.issueAccessToken(principal()).value();

        assertThatThrownBy(() -> ours.parse(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("tampering with the payload invalidates the signature")
    void rejectsTamperedPayload() {
        JwtService jwt = serviceWith(SECRET, "skool");
        String token = jwt.issueAccessToken(principal()).value();

        String[] parts = token.split("\\.");
        String tamperedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + UUID.randomUUID() + "\",\"roles\":[\"ADMIN\"]}")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a short secret fails fast at construction, not at first login")
    void refusesWeakSecret() {
        assertThatThrownBy(() -> serviceWith("too-short", "skool"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("an expired token is rejected")
    void rejectsExpiredToken() throws Exception {
        JwtService jwt = new JwtService(
                new JwtProperties("skool", SECRET, Duration.ofMillis(1), Duration.ofDays(30)));
        String token = jwt.issueAccessToken(principal()).value();
        Thread.sleep(50);

        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(JwtException.class);
    }
}
