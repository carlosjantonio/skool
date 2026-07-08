package ao.skool.assessment.internal.persistence;

import ao.skool.assessment.internal.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, QuizQuestion.PK> {

    List<QuizQuestion> findByQuizIdOrderByPositionAsc(UUID quizId);
}
