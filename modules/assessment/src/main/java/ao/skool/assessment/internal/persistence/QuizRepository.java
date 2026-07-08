package ao.skool.assessment.internal.persistence;

import ao.skool.assessment.internal.domain.Quiz;
import ao.skool.assessment.internal.domain.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByTenantIdAndTurmaIdOrderByCreatedAtDesc(UUID tenantId, UUID turmaId);

    List<Quiz> findByTurmaIdAndStatusOrderByCreatedAtDesc(UUID turmaId, QuizStatus status);
}
