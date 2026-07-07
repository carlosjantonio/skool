package ao.skool.sis.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guardians",
       indexes = {
           @Index(name = "idx_guardians_tenant", columnList = "tenant_id"),
           @Index(name = "idx_guardians_email", columnList = "email")
       })
public class Guardian {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(length = 14)
    private String bi;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(length = 255)
    private String email;

    /** Populated when a portal user is provisioned for this guardian. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Guardian() {}

    public Guardian(UUID id, UUID tenantId, String fullName, String phone) {
        this.id = id;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.phone = phone;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String bi() { return bi; }
    public String fullName() { return fullName; }
    public String phone() { return phone; }
    public String email() { return email; }
    public UUID userId() { return userId; }

    public void setBi(String bi) { this.bi = bi; }
    public void setEmail(String email) { this.email = email; }
    public void linkUser(UUID userId) { this.userId = userId; }
}
