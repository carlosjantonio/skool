package ao.skool.assessment.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quizzes",
       indexes = {
           @Index(name = "idx_quizzes_tenant_turma", columnList = "tenant_id, turma_id"),
           @Index(name = "idx_quizzes_subject", columnList = "subject_id")
       })
public class Quiz {

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
    private String instructions;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "randomize_questions", nullable = false)
    private boolean randomizeQuestions = true;

    @Column(name = "randomize_options", nullable = false)
    private boolean randomizeOptions = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuizStatus status = QuizStatus.DRAFT;

    @Column(name = "opens_at")
    private Instant opensAt;

    @Column(name = "closes_at")
    private Instant closesAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    protected Quiz() {}

    public Quiz(UUID id, UUID tenantId, UUID subjectId, UUID turmaId, UUID academicYearId,
                String trimesterKey, String title, String instructions,
                Integer timeLimitSeconds, boolean randomizeQuestions, boolean randomizeOptions,
                Instant opensAt, Instant closesAt, UUID createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.subjectId = subjectId;
        this.turmaId = turmaId;
        this.academicYearId = academicYearId;
        this.trimesterKey = trimesterKey;
        this.title = title;
        this.instructions = instructions;
        this.timeLimitSeconds = timeLimitSeconds;
        this.randomizeQuestions = randomizeQuestions;
        this.randomizeOptions = randomizeOptions;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.createdBy = createdBy;
    }

    public void publish() {
        this.status = QuizStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void close() {
        this.status = QuizStatus.CLOSED;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID subjectId() { return subjectId; }
    public UUID turmaId() { return turmaId; }
    public UUID academicYearId() { return academicYearId; }
    public String trimesterKey() { return trimesterKey; }
    public String title() { return title; }
    public String instructions() { return instructions; }
    public Integer timeLimitSeconds() { return timeLimitSeconds; }
    public boolean randomizeQuestions() { return randomizeQuestions; }
    public boolean randomizeOptions() { return randomizeOptions; }
    public QuizStatus status() { return status; }
    public Instant opensAt() { return opensAt; }
    public Instant closesAt() { return closesAt; }
    public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
    public Instant publishedAt() { return publishedAt; }
}
