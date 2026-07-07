package ao.skool.staff.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff",
       indexes = {
           @Index(name = "idx_staff_tenant", columnList = "tenant_id"),
           @Index(name = "idx_staff_user", columnList = "user_id")
       })
public class Staff {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(length = 14)
    private String bi;

    @Column(length = 10)
    private String nif;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 32)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "qualification", length = 255)
    private String qualification;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    /** Filled when a portal login is provisioned. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Staff() {}

    public Staff(UUID id, UUID tenantId, String fullName) {
        this.id = id;
        this.tenantId = tenantId;
        this.fullName = fullName;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String bi() { return bi; }
    public String nif() { return nif; }
    public String fullName() { return fullName; }
    public String phone() { return phone; }
    public String email() { return email; }
    public String qualification() { return qualification; }
    public LocalDate hireDate() { return hireDate; }
    public UUID userId() { return userId; }
    public boolean active() { return active; }

    public void setBi(String bi) { this.bi = bi; }
    public void setNif(String nif) { this.nif = nif; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setQualification(String q) { this.qualification = q; }
    public void setHireDate(LocalDate d) { this.hireDate = d; }
    public void linkUser(UUID userId) { this.userId = userId; }
    public void deactivate() { this.active = false; }
}
