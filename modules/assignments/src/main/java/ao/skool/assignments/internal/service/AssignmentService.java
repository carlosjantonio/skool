package ao.skool.assignments.internal.service;

import ao.skool.assignments.api.event.AssignmentDue;
import ao.skool.assignments.internal.domain.Assignment;
import ao.skool.assignments.internal.domain.AssignmentSubmission;
import ao.skool.assignments.internal.persistence.AssignmentRepository;
import ao.skool.assignments.internal.persistence.SubmissionRepository;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.AssignmentResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.CreateAssignment;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.GradeSubmission;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.SubmissionResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.SubmitAssignment;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.TeacherAssignmentDetail;
import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.sis.api.StudentDirectory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final StudentDirectory studentDirectory;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;

    public AssignmentService(AssignmentRepository assignments, SubmissionRepository submissions,
                             StudentDirectory studentDirectory, TenantContext tenant,
                             ApplicationEventPublisher events) {
        this.assignments = assignments;
        this.submissions = submissions;
        this.studentDirectory = studentDirectory;
        this.tenant = tenant;
        this.events = events;
    }

    public AssignmentResponse create(CreateAssignment cmd) {
        UUID tenantId = tenant.current().value();
        BigDecimal maxScore = cmd.maxScore() != null ? cmd.maxScore() : new BigDecimal("20.00");
        boolean allowLate = cmd.allowLate() == null || cmd.allowLate();
        Assignment a = new Assignment(UUID.randomUUID(), tenantId, cmd.subjectId(), cmd.turmaId(),
                cmd.academicYearId(), cmd.trimesterKey(), cmd.title(), cmd.description(),
                cmd.rubric(), maxScore, cmd.dueAt(), allowLate, currentActor());
        assignments.save(a);
        events.publishEvent(new AssignmentDue(UUID.randomUUID(), tenant.current(),
                a.id(), a.subjectId(), a.turmaId(), a.title(), a.dueAt(), Instant.now()));
        return toResponse(a, 0);
    }

    public AssignmentResponse close(UUID id) {
        Assignment a = assignments.findById(id).orElseThrow(NotFoundException::new);
        assertOwned(a);
        a.close();
        int count = submissions.findByAssignmentIdOrderBySubmittedAtAsc(id).size();
        return toResponse(a, count);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(UUID id) {
        Assignment a = assignments.findById(id).orElseThrow(NotFoundException::new);
        assertOwned(a);
        int count = submissions.findByAssignmentIdOrderBySubmittedAtAsc(id).size();
        return toResponse(a, count);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listByTurma(UUID turmaId) {
        UUID tenantId = tenant.current().value();
        return assignments.findByTenantIdAndTurmaIdOrderByDueAtDesc(tenantId, turmaId).stream()
                .map(a -> toResponse(a, submissions.findByAssignmentIdOrderBySubmittedAtAsc(a.id()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherAssignmentDetail teacherDetail(UUID id) {
        Assignment a = assignments.findById(id).orElseThrow(NotFoundException::new);
        assertOwned(a);
        var subs = submissions.findByAssignmentIdOrderBySubmittedAtAsc(id);
        Map<UUID, String> names = new HashMap<>();
        for (var s : subs) {
            studentDirectory.findStudent(s.studentId())
                    .ifPresent(v -> names.put(s.studentId(), v.fullName()));
        }
        List<SubmissionResponse> rows = subs.stream()
                .map(s -> toSubmission(s, names.getOrDefault(s.studentId(), "?")))
                .toList();
        return new TeacherAssignmentDetail(toResponse(a, subs.size()), rows);
    }

    public SubmissionResponse submit(UUID assignmentId, UUID studentId, SubmitAssignment cmd) {
        Assignment a = assignments.findById(assignmentId).orElseThrow(NotFoundException::new);
        if (!a.tenantId().equals(tenant.current().value())) throw new NotFoundException();

        Instant now = Instant.now();
        boolean isLate = now.isAfter(a.dueAt());
        if (isLate && !a.allowLate()) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }

        AssignmentSubmission existing = submissions.findByAssignmentIdAndStudentId(assignmentId, studentId).orElse(null);
        AssignmentSubmission sub;
        if (existing != null) {
            existing.resubmit(cmd.documentId(), cmd.notes(), isLate);
            sub = existing;
        } else {
            sub = new AssignmentSubmission(UUID.randomUUID(), tenant.current().value(),
                    assignmentId, studentId, cmd.documentId(), cmd.notes(), isLate);
            submissions.save(sub);
        }
        return toSubmission(sub, null);
    }

    public SubmissionResponse grade(UUID submissionId, GradeSubmission cmd) {
        AssignmentSubmission s = submissions.findById(submissionId).orElseThrow(NotFoundException::new);
        if (!s.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        s.applyGrade(cmd.score(), cmd.feedback(), currentActor());
        String name = studentDirectory.findStudent(s.studentId())
                .map(StudentDirectory.StudentSummary::fullName).orElse("?");
        return toSubmission(s, name);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse mySubmission(UUID assignmentId, UUID studentId) {
        var s = submissions.findByAssignmentIdAndStudentId(assignmentId, studentId).orElse(null);
        return s == null ? null : toSubmission(s, null);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> mySubmissions(UUID studentId) {
        return submissions.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .map(s -> toSubmission(s, null))
                .toList();
    }

    private void assertOwned(Assignment a) {
        if (!a.tenantId().equals(tenant.current().value())) throw new NotFoundException();
    }

    private AssignmentResponse toResponse(Assignment a, int count) {
        return new AssignmentResponse(a.id(), a.subjectId(), a.turmaId(), a.academicYearId(),
                a.trimesterKey(), a.title(), a.description(), a.rubric(), a.maxScore(),
                a.dueAt(), a.allowLate(), a.status(), a.createdAt(), count);
    }

    private SubmissionResponse toSubmission(AssignmentSubmission s, String studentName) {
        return new SubmissionResponse(s.id(), s.assignmentId(), s.studentId(), s.documentId(),
                s.notes(), s.submittedAt(), s.isLate(), s.score(), s.feedback(), s.gradedAt(),
                s.status(), studentName);
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            try { return UUID.fromString(p.userId()); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
}
