package ao.skool.assessment.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A reusable item in the question bank. Payload is a JSON string whose shape depends
 * on {@link #questionType}; see {@code V9_1__assessment.sql} for the schema.
 */
@Entity
@Table(name = "questions",
       indexes = {
           @Index(name = "idx_questions_tenant_subject", columnList = "tenant_id, subject_id"),
           @Index(name = "idx_questions_grade_level", columnList = "grade_level")
       })
public class Question {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "grade_level", nullable = false, length = 16)
    private String gradeLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 24)
    private QuestionType questionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal points = BigDecimal.ONE;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Question() {}

    public Question(UUID id, UUID tenantId, UUID subjectId, String gradeLevel, String prompt,
                    QuestionType questionType, String payload, BigDecimal points, UUID createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.subjectId = subjectId;
        this.gradeLevel = gradeLevel;
        this.prompt = prompt;
        this.questionType = questionType;
        this.payload = payload;
        this.points = points == null ? BigDecimal.ONE : points;
        this.createdBy = createdBy;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID subjectId() { return subjectId; }
    public String gradeLevel() { return gradeLevel; }
    public String prompt() { return prompt; }
    public QuestionType questionType() { return questionType; }
    public String payload() { return payload; }
    public BigDecimal points() { return points; }
    public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
}
