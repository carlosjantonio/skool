package ao.skool.assignments.internal.web;

import ao.skool.assignments.internal.service.AssignmentService;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.AssignmentResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.CreateAssignment;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.GradeSubmission;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.SubmissionResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.TeacherAssignmentDetail;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private static final String TEACHER_ROLES = "hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')";

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) { this.service = service; }

    @PostMapping
    @PreAuthorize(TEACHER_ROLES)
    public AssignmentResponse create(@Valid @RequestBody CreateAssignment cmd) {
        return service.create(cmd);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize(TEACHER_ROLES)
    public AssignmentResponse close(@PathVariable UUID id) {
        return service.close(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize(TEACHER_ROLES)
    public AssignmentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize(TEACHER_ROLES)
    public TeacherAssignmentDetail detail(@PathVariable UUID id) {
        return service.teacherDetail(id);
    }

    @GetMapping
    @PreAuthorize(TEACHER_ROLES)
    public List<AssignmentResponse> listByTurma(@RequestParam UUID turmaId) {
        return service.listByTurma(turmaId);
    }

    @PostMapping("/submissions/{submissionId}/grade")
    @PreAuthorize(TEACHER_ROLES)
    public SubmissionResponse grade(@PathVariable UUID submissionId, @Valid @RequestBody GradeSubmission cmd) {
        return service.grade(submissionId, cmd);
    }
}
