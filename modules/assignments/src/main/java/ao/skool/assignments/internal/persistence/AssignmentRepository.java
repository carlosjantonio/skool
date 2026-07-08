package ao.skool.assignments.internal.persistence;

import ao.skool.assignments.internal.domain.Assignment;
import ao.skool.assignments.internal.domain.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByTenantIdAndTurmaIdOrderByDueAtDesc(UUID tenantId, UUID turmaId);

    List<Assignment> findByTurmaIdAndStatusOrderByDueAtAsc(UUID turmaId, AssignmentStatus status);

    List<Assignment> findByTenantIdAndDueAtBetween(UUID tenantId, Instant from, Instant to);
}
