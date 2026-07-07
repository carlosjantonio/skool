package ao.skool.identity.internal.web.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID userId,
        UUID tenantId,
        String email,
        String fullName,
        Set<String> roles
) {}
