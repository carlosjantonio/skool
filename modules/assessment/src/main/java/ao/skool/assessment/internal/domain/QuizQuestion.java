package ao.skool.assessment.internal.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Ordered link between a quiz and a question from the bank. Uses a composite
 * (quiz_id, question_id) primary key — the same question can appear in many
 * quizzes but not twice in the same one.
 */
@Entity
@Table(name = "quiz_questions",
       indexes = @Index(name = "idx_quiz_questions_quiz", columnList = "quiz_id, position"))
@IdClass(QuizQuestion.PK.class)
public class QuizQuestion {

    @Id
    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Id
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private int position;

    protected QuizQuestion() {}

    public QuizQuestion(UUID quizId, UUID questionId, int position) {
        this.quizId = quizId;
        this.questionId = questionId;
        this.position = position;
    }

    public UUID quizId() { return quizId; }
    public UUID questionId() { return questionId; }
    public int position() { return position; }

    public static class PK implements Serializable {
        private UUID quizId;
        private UUID questionId;

        public PK() {}
        public PK(UUID quizId, UUID questionId) {
            this.quizId = quizId;
            this.questionId = questionId;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(quizId, pk.quizId) && Objects.equals(questionId, pk.questionId);
        }
        @Override public int hashCode() { return Objects.hash(quizId, questionId); }
    }
}
