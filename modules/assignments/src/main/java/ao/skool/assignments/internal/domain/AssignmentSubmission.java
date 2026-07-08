package ao.skool.assignments.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_submissions",
       uniqueConstraints = @UniqueConstraint(name = "uk_submissions_assignment_student",
                                              columnNames = {"assignment_id", "student_id"}),
       indexes = {
           @Index(name = "idx_submissions_tenant_student", columnList = "tenant_id, student_id"),
           @Index(name = "idx_submissions_assignment", columnList = "assignment_id")
       })
public class AssignmentSubmission {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /** Optional link into the documents module — the actual file lives in MinIO. */
    @Column(name = "document_id")
    private UUID documentId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "is_late", nullable = false)
    private boolean isLate = false;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    protected AssignmentSubmission() {}

    public AssignmentSubmission(UUID id, UUID tenantId, UUID assignmentId, UUID studentId,
                                 UUID documentId, String notes, boolean isLate) {
        this.id = id;
        this.tenantId = tenantId;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.documentId = documentId;
        this.notes = notes;
        this.isLate = isLate;
    }

    public void resubmit(UUID documentId, String notes, boolean isLate) {
        this.documentId = documentId;
        this.notes = notes;
        this.isLate = isLate;
        this.submittedAt = Instant.now();
        // Re-open for grading.
        this.status = SubmissionStatus.SUBMITTED;
        this.score = null;
        this.feedback = null;
        this.gradedAt = null;
        this.gradedBy = null;
    }

    public void applyGrade(BigDecimal score, String feedback, UUID gradedBy) {
        this.score = score;
        this.feedback = feedback;
        this.gradedBy = gradedBy;
        this.gradedAt = Instant.now();
        this.status = SubmissionStatus.GRADED;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID assignmentId() { return assignmentId; }
    public UUID studentId() { return studentId; }
    public UUID documentId() { return documentId; }
    public String notes() { return notes; }
    public Instant submittedAt() { return submittedAt; }
    public boolean isLate() { return isLate; }
    public BigDecimal score() { return score; }
    public String feedback() { return feedback; }
    public UUID gradedBy() { return gradedBy; }
    public Instant gradedAt() { return gradedAt; }
    public SubmissionStatus status() { return status; }
}
