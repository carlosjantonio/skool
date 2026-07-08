package ao.skool.assessment.internal.persistence;

import ao.skool.assessment.internal.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByTenantIdAndSubjectIdOrderByCreatedAtDesc(UUID tenantId, UUID subjectId);

    List<Question> findByTenantIdAndSubjectIdAndGradeLevelOrderByCreatedAtDesc(
            UUID tenantId, UUID subjectId, String gradeLevel);
}
