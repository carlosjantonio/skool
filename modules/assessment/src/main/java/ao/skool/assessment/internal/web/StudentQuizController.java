package ao.skool.assessment.internal.web;

import ao.skool.assessment.internal.service.AttemptService;
import ao.skool.assessment.internal.service.QuizService;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.AttemptResult;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.AttemptView;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizAttemptRow;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizResponse;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.SubmitAttempt;
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

/**
 * Endpoints scoped to the currently authenticated student. The student profile is
 * resolved via {@link StudentDirectory#findStudentByUserId(UUID)} — no {@code studentId}
 * ever appears in the URL, so a student can never poke at another student's attempt.
 */
@RestController
@RequestMapping("/api/student/quizzes")
public class StudentQuizController {

    private final QuizService quizzes;
    private final AttemptService attempts;
    private final StudentDirectory studentDirectory;

    public StudentQuizController(QuizService quizzes, AttemptService attempts,
                                  StudentDirectory studentDirectory) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.studentDirectory = studentDirectory;
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public List<QuizResponse> available(@RequestParam UUID turmaId) {
        return quizzes.listAvailable(turmaId);
    }

    @PostMapping("/{quizId}/attempts")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public AttemptView start(@AuthenticationPrincipal SkoolPrincipal principal,
                              @PathVariable UUID quizId) {
        UUID studentId = studentIdFor(principal);
        return attempts.startOrResume(quizId, studentId);
    }

    @PutMapping("/attempts/{attemptId}")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public AttemptResult saveAnswers(@AuthenticationPrincipal SkoolPrincipal principal,
                                      @PathVariable UUID attemptId,
                                      @Valid @RequestBody SubmitAttempt cmd) {
        UUID studentId = studentIdFor(principal);
        return attempts.submit(attemptId, studentId, cmd, false);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public AttemptResult submit(@AuthenticationPrincipal SkoolPrincipal principal,
                                 @PathVariable UUID attemptId,
                                 @Valid @RequestBody SubmitAttempt cmd) {
        UUID studentId = studentIdFor(principal);
        return attempts.submit(attemptId, studentId, cmd, true);
    }

    @GetMapping("/attempts")
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public List<QuizAttemptRow> myAttempts(@AuthenticationPrincipal SkoolPrincipal principal) {
        UUID studentId = studentIdFor(principal);
        return attempts.attemptsForStudent(studentId);
    }

    private UUID studentIdFor(SkoolPrincipal principal) {
        UUID userId = UUID.fromString(principal.userId());
        return studentDirectory.findStudentByUserId(userId)
                .map(StudentDirectory.StudentSummary::id)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));
    }
}
