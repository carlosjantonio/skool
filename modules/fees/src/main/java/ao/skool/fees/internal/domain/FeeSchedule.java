package ao.skool.fees.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_schedules",
       indexes = @Index(name = "idx_fee_schedules_tenant_year", columnList = "tenant_id, academic_year_id"))
public class FeeSchedule {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private FeeKind kind;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "AOA";

    @Column(name = "grade_level", length = 16)
    private String gradeLevel;

    @Column(name = "trimester_key", length = 4)
    private String trimesterKey;

    /** 1-12 for propina mensal; null for one-off fees. */
    @Column(name = "period_month")
    private Integer periodMonth;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected FeeSchedule() {}

    public FeeSchedule(UUID id, UUID tenantId, UUID academicYearId, String name, FeeKind kind,
                       BigDecimal amount, String gradeLevel, String trimesterKey,
                       Integer periodMonth, Instant dueAt, UUID createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.academicYearId = academicYearId;
        this.name = name;
        this.kind = kind;
        this.amount = amount;
        this.gradeLevel = gradeLevel;
        this.trimesterKey = trimesterKey;
        this.periodMonth = periodMonth;
        this.dueAt = dueAt;
        this.createdBy = createdBy;
    }

    public void retire() { this.active = false; }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID academicYearId() { return academicYearId; }
    public String name() { return name; }
    public FeeKind kind() { return kind; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public String gradeLevel() { return gradeLevel; }
    public String trimesterKey() { return trimesterKey; }
    public Integer periodMonth() { return periodMonth; }
    public Instant dueAt() { return dueAt; }
    public boolean active() { return active; }
    public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
}
