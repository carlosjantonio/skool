package ao.skool.identity.internal.service;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.TenantId;
import ao.skool.common.security.JwtProperties;
import ao.skool.common.security.jwt.JwtService;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.identity.internal.domain.RefreshToken;
import ao.skool.identity.internal.domain.User;
import ao.skool.identity.internal.persistence.RefreshTokenRepository;
import ao.skool.identity.internal.persistence.UserRepository;
import ao.skool.identity.internal.web.dto.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProps;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProps) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProps = jwtProps;
    }

    public TokenResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);
        if (!user.active() || !passwordEncoder.matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }
        return issueTokens(user);
    }

    public TokenResponse refresh(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = refreshTokens.findByTokenHash(hash)
                .orElseThrow(this::invalidCredentials);
        if (!token.isActive(Instant.now())) {
            throw invalidCredentials();
        }
        User user = users.findById(token.userId()).orElseThrow(this::invalidCredentials);
        if (!user.active()) {
            throw invalidCredentials();
        }
        // Rotate: revoke the used refresh token and issue a fresh pair.
        token.revoke();
        refreshTokens.save(token);
        return issueTokens(user);
    }

    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(sha256(rawToken)).ifPresent(t -> {
            t.revoke();
            refreshTokens.save(t);
        });
    }

    private TokenResponse issueTokens(User user) {
        SkoolPrincipal principal = new SkoolPrincipal(
                user.id().toString(),
                TenantId.of(user.tenantId()),
                user.email(),
                user.fullName(),
                user.roles());
        var access = jwtService.issueAccessToken(principal);

        String rawRefresh = generateOpaqueToken();
        Instant refreshExpires = Instant.now().plus(jwtProps.refreshTokenTtl());
        refreshTokens.save(new RefreshToken(UUID.randomUUID(), user.id(), sha256(rawRefresh), refreshExpires));

        return new TokenResponse(
                access.value(), access.expiresAt(),
                rawRefresh, refreshExpires,
                user.id(), user.tenantId(),
                user.email(), user.fullName(), user.roles()
        );
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ApplicationException invalidCredentials() {
        return new ApplicationException(HttpStatus.UNAUTHORIZED, "error.unauthorized");
    }
}
