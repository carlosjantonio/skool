package ao.skool.assessment.internal.persistence;

import ao.skool.assessment.internal.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByQuizIdAndStudentId(UUID quizId, UUID studentId);

    List<QuizAttempt> findByQuizIdOrderByStartedAtAsc(UUID quizId);

    List<QuizAttempt> findByStudentIdOrderByStartedAtDesc(UUID studentId);
}
