package ao.skool.common.security.jwt;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.TenantId;
import ao.skool.common.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "skool.security.jwt.secret must be at least 32 bytes for HS256; got " + secret.length);
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public IssuedToken issueAccessToken(SkoolPrincipal principal) {
        Instant now = Instant.now();
        Instant expires = now.plus(props.accessTokenTtl());
        String jwt = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.issuer())
                .subject(principal.userId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .claim("tenant", principal.tenantId().value().toString())
                .claim("email", principal.email())
                .claim("roles", List.copyOf(principal.roles()))
                .signWith(key)
                .compact();
        return new IssuedToken(jwt, expires);
    }

    public SkoolPrincipal parse(String jwt) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return new SkoolPrincipal(
                claims.getSubject(),
                TenantId.of(claims.get("tenant", String.class)),
                claims.get("email", String.class),
                Set.copyOf(roles)
        );
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
