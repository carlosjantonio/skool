package ao.skool.identity.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens",
       indexes = @Index(name = "idx_refresh_tokens_user", columnList = "user_id"))
public class RefreshToken {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RefreshToken() {}

    public RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String tokenHash() { return tokenHash; }
    public Instant expiresAt() { return expiresAt; }
    public Instant revokedAt() { return revokedAt; }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
