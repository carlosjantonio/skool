package ao.skool.grading.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single grade entry on the Angolan 0–20 scale. Multiple entries per (student,
 * subject, trimester) are expected — averages are computed from them, not stored.
 */
@Entity
@Table(name = "grades",
       indexes = {
           @Index(name = "idx_grades_student_year", columnList = "student_id, academic_year_id"),
           @Index(name = "idx_grades_turma_subject", columnList = "turma_id, subject_id"),
           @Index(name = "idx_grades_tenant_year", columnList = "tenant_id, academic_year_id")
       })
public class Grade {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "trimester_key", nullable = false, length = 4)
    private String trimesterKey;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal value;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GradeCategory category;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt = Instant.now();

    protected Grade() {}

    public Grade(UUID id, UUID tenantId, UUID studentId, UUID subjectId, UUID turmaId,
                 UUID academicYearId, String trimesterKey, BigDecimal value, BigDecimal weight,
                 GradeCategory category, String notes, UUID recordedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.turmaId = turmaId;
        this.academicYearId = academicYearId;
        this.trimesterKey = trimesterKey;
        this.value = value;
        this.weight = weight == null ? BigDecimal.ONE : weight;
        this.category = category;
        this.notes = notes;
        this.recordedBy = recordedBy;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID studentId() { return studentId; }
    public UUID subjectId() { return subjectId; }
    public UUID turmaId() { return turmaId; }
    public UUID academicYearId() { return academicYearId; }
    public String trimesterKey() { return trimesterKey; }
    public BigDecimal value() { return value; }
    public BigDecimal weight() { return weight; }
    public GradeCategory category() { return category; }
    public String notes() { return notes; }
    public UUID recordedBy() { return recordedBy; }
    public Instant recordedAt() { return recordedAt; }
}
