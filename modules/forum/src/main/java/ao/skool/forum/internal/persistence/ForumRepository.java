package ao.skool.forum.internal.persistence;

import ao.skool.forum.internal.domain.Forum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForumRepository extends JpaRepository<Forum, UUID> {

    Optional<Forum> findBySubjectIdAndTurmaIdAndAcademicYearId(UUID subjectId, UUID turmaId, UUID academicYearId);

    List<Forum> findByTenantIdAndTurmaIdOrderByTitleAsc(UUID tenantId, UUID turmaId);
}
