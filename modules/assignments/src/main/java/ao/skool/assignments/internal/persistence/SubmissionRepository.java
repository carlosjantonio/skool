package ao.skool.assignments.internal.persistence;

import ao.skool.assignments.internal.domain.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtAsc(UUID assignmentId);

    List<AssignmentSubmission> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);
}
