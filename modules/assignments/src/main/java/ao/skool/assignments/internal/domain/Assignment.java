package ao.skool.assignments.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignments",
       indexes = {
           @Index(name = "idx_assignments_tenant_turma", columnList = "tenant_id, turma_id"),
           @Index(name = "idx_assignments_due", columnList = "due_at")
       })
public class Assignment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "trimester_key", nullable = false, length = 4)
    private String trimesterKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String rubric;

    @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore = new BigDecimal("20.00");

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "allow_late", nullable = false)
    private boolean allowLate = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssignmentStatus status = AssignmentStatus.OPEN;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Assignment() {}

    public Assignment(UUID id, UUID tenantId, UUID subjectId, UUID turmaId, UUID academicYearId,
                      String trimesterKey, String title, String description, String rubric,
                      BigDecimal maxScore, Instant dueAt, boolean allowLate, UUID createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.subjectId = subjectId;
        this.turmaId = turmaId;
        this.academicYearId = academicYearId;
        this.trimesterKey = trimesterKey;
        this.title = title;
        this.description = description;
        this.rubric = rubric;
        this.maxScore = maxScore == null ? new BigDecimal("20.00") : maxScore;
        this.dueAt = dueAt;
        this.allowLate = allowLate;
        this.createdBy = createdBy;
    }

    public void close() { this.status = AssignmentStatus.CLOSED; }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID subjectId() { return subjectId; }
    public UUID turmaId() { return turmaId; }
    public UUID academicYearId() { return academicYearId; }
    public String trimesterKey() { return trimesterKey; }
    public String title() { return title; }
    public String description() { return description; }
    public String rubric() { return rubric; }
    public BigDecimal maxScore() { return maxScore; }
    public Instant dueAt() { return dueAt; }
    public boolean allowLate() { return allowLate; }
    public AssignmentStatus status() { return status; }
    public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
}
