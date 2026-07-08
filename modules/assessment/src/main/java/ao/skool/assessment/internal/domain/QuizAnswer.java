package ao.skool.assessment.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_answers",
       uniqueConstraints = @UniqueConstraint(name = "uk_answers_attempt_question",
                                              columnNames = {"attempt_id", "question_id"}),
       indexes = @Index(name = "idx_answers_attempt", columnList = "attempt_id"))
public class QuizAnswer {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "points_earned", precision = 4, scale = 2)
    private BigDecimal pointsEarned;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt = Instant.now();

    protected QuizAnswer() {}

    public QuizAnswer(UUID id, UUID attemptId, UUID questionId, String response) {
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.response = response;
    }

    public void applyAutoGrade(boolean correct, BigDecimal pointsEarned) {
        this.isCorrect = correct;
        this.pointsEarned = pointsEarned;
    }

    public void applyManualGrade(BigDecimal pointsEarned, String feedback) {
        this.pointsEarned = pointsEarned;
        this.teacherFeedback = feedback;
    }

    public void updateResponse(String response) {
        this.response = response;
        this.answeredAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID attemptId() { return attemptId; }
    public UUID questionId() { return questionId; }
    public String response() { return response; }
    public Boolean isCorrect() { return isCorrect; }
    public BigDecimal pointsEarned() { return pointsEarned; }
    public String teacherFeedback() { return teacherFeedback; }
    public Instant answeredAt() { return answeredAt; }
}
