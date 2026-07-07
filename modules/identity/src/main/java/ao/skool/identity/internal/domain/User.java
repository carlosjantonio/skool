package ao.skool.identity.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles",
                     joinColumns = @JoinColumn(name = "user_id", nullable = false),
                     uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role"}))
    @Column(name = "role", nullable = false, length = 32)
    private Set<String> roles = new HashSet<>();

    protected User() {}

    public User(UUID id, UUID tenantId, String email, String passwordHash, String fullName, Set<String> roles) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roles = new HashSet<>(roles);
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String fullName() { return fullName; }
    public boolean active() { return active; }
    public Set<String> roles() { return Set.copyOf(roles); }

    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public void deactivate() { this.active = false; }
}
