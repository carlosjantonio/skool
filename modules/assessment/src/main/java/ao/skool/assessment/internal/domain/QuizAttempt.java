package ao.skool.assessment.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts",
       uniqueConstraints = @UniqueConstraint(name = "uk_attempts_quiz_student",
                                              columnNames = {"quiz_id", "student_id"}),
       indexes = {
           @Index(name = "idx_attempts_tenant_student", columnList = "tenant_id, student_id"),
           @Index(name = "idx_attempts_quiz", columnList = "quiz_id")
       })
public class QuizAttempt {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "auto_score", precision = 6, scale = 2)
    private BigDecimal autoScore;

    @Column(name = "manual_score", precision = 6, scale = 2)
    private BigDecimal manualScore;

    @Column(name = "total_points", precision = 6, scale = 2)
    private BigDecimal totalPoints;

    @Column(name = "question_order", nullable = false, columnDefinition = "TEXT")
    private String questionOrder;

    @Column(name = "tab_switch_count", nullable = false)
    private int tabSwitchCount = 0;

    protected QuizAttempt() {}

    public QuizAttempt(UUID id, UUID tenantId, UUID quizId, UUID studentId, String questionOrder) {
        this.id = id;
        this.tenantId = tenantId;
        this.quizId = quizId;
        this.studentId = studentId;
        this.questionOrder = questionOrder;
    }

    public void markSubmitted(BigDecimal autoScore, BigDecimal totalPoints, boolean hasEssay) {
        this.status = hasEssay ? AttemptStatus.SUBMITTED : AttemptStatus.GRADED;
        this.autoScore = autoScore;
        this.totalPoints = totalPoints;
        this.submittedAt = Instant.now();
    }

    public void applyManualScore(BigDecimal manualScore) {
        this.manualScore = manualScore;
        this.status = AttemptStatus.GRADED;
    }

    public void incrementTabSwitch() { this.tabSwitchCount++; }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID quizId() { return quizId; }
    public UUID studentId() { return studentId; }
    public AttemptStatus status() { return status; }
    public Instant startedAt() { return startedAt; }
    public Instant submittedAt() { return submittedAt; }
    public BigDecimal autoScore() { return autoScore; }
    public BigDecimal manualScore() { return manualScore; }
    public BigDecimal totalPoints() { return totalPoints; }
    public String questionOrder() { return questionOrder; }
    public int tabSwitchCount() { return tabSwitchCount; }
}
