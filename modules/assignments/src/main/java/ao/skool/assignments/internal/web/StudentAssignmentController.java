package ao.skool.assignments.internal.web;

import ao.skool.assignments.internal.service.AssignmentService;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.AssignmentResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.SubmissionResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.SubmitAssignment;
import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.security.Roles;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.sis.api.StudentDirectory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/assignments")
public class StudentAssignmentController {

    private final AssignmentService service;
    private final StudentDirectory studentDirectory;

    public StudentAssignmentController(AssignmentService service, StudentDirectory studentDirectory) {
        this.service = service;
        this.studentDirectory = studentDirectory;
    }

    @GetMapping
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public List<AssignmentResponse> forTurma(@RequestParam UUID turmaId) {
        return service.listByTurma(turmaId);
    }

    @PostMapping("/{assignmentId}/submit")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public SubmissionResponse submit(@AuthenticationPrincipal SkoolPrincipal principal,
                                       @PathVariable UUID assignmentId,
                                       @Valid @RequestBody SubmitAssignment cmd) {
        UUID studentId = studentIdFor(principal);
        return service.submit(assignmentId, studentId, cmd);
    }

    @GetMapping("/{assignmentId}/mine")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public SubmissionResponse mySubmission(@AuthenticationPrincipal SkoolPrincipal principal,
                                            @PathVariable UUID assignmentId) {
        UUID studentId = studentIdFor(principal);
        SubmissionResponse s = service.mySubmission(assignmentId, studentId);
        if (s == null) throw new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found");
        return s;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public List<SubmissionResponse> mine(@AuthenticationPrincipal SkoolPrincipal principal) {
        UUID studentId = studentIdFor(principal);
        return service.mySubmissions(studentId);
    }

    private UUID studentIdFor(SkoolPrincipal principal) {
        UUID userId = UUID.fromString(principal.userId());
        return studentDirectory.findStudentByUserId(userId)
                .map(StudentDirectory.StudentSummary::id)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));
    }
}
