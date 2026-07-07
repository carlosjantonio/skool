package ao.skool.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "skool.security.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) issuer = "skool";
        if (accessTokenTtl == null) accessTokenTtl = Duration.ofMinutes(30);
        if (refreshTokenTtl == null) refreshTokenTtl = Duration.ofDays(30);
    }
}
