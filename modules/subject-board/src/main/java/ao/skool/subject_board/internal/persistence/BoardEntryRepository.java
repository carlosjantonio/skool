package ao.skool.subject_board.internal.persistence;

import ao.skool.subject_board.internal.domain.BoardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardEntryRepository extends JpaRepository<BoardEntry, UUID> {

    List<BoardEntry> findBySubjectIdAndTurmaIdOrderByPinnedDescPublishedAtDesc(UUID subjectId, UUID turmaId);

    List<BoardEntry> findByTenantIdAndTurmaIdOrderByPinnedDescPublishedAtDesc(UUID tenantId, UUID turmaId);
}
