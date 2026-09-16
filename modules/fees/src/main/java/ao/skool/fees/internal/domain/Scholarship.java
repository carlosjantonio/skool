package ao.skool.fees.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scholarships",
       indexes = @Index(name = "idx_scholarships_tenant_student", columnList = "tenant_id, student_id"))
public class Scholarship {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ScholarshipKind kind;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "fixed_amount", precision = 14, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Scholarship() {}

    public Scholarship(UUID id, UUID tenantId, UUID studentId, ScholarshipKind kind,
                       BigDecimal percentage, BigDecimal fixedAmount,
                       LocalDate validFrom, LocalDate validTo, String reason) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.kind = kind;
        this.percentage = percentage;
        this.fixedAmount = fixedAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.reason = reason;
    }

    /** Returns the discount to apply to an invoice with the given gross amount. */
    public BigDecimal discountFor(BigDecimal gross) {
        return switch (kind) {
            case FULL -> gross;
            case PERCENTAGE -> {
                BigDecimal pct = percentage == null ? BigDecimal.ZERO : percentage;
                yield gross.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            case FIXED -> {
                BigDecimal fx = fixedAmount == null ? BigDecimal.ZERO : fixedAmount;
                // Cap the discount at the gross so we never go negative.
                yield fx.min(gross);
            }
        };
    }

    public boolean isValidOn(LocalDate date) {
        if (!active) return false;
        if (validFrom.isAfter(date)) return false;
        return validTo == null || !validTo.isBefore(date);
    }

    public void deactivate() { this.active = false; }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID studentId() { return studentId; }
    public ScholarshipKind kind() { return kind; }
    public BigDecimal percentage() { return percentage; }
    public BigDecimal fixedAmount() { return fixedAmount; }
    public LocalDate validFrom() { return validFrom; }
    public LocalDate validTo() { return validTo; }
    public String reason() { return reason; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
}
