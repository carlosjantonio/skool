package ao.skool.assessment.internal.web;

import ao.skool.assessment.internal.service.AttemptService;
import ao.skool.assessment.internal.service.QuizService;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.CreateQuiz;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.ManualGradeBatch;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizAnalytics;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizAttemptRow;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuizResponse;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private static final String TEACHER_ROLES = "hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')";

    private final QuizService quizzes;
    private final AttemptService attempts;

    public QuizController(QuizService quizzes, AttemptService attempts) {
        this.quizzes = quizzes;
        this.attempts = attempts;
    }

    @PostMapping
    @PreAuthorize(TEACHER_ROLES)
    public QuizResponse create(@Valid @RequestBody CreateQuiz cmd) {
        return quizzes.create(cmd);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize(TEACHER_ROLES)
    public QuizResponse publish(@PathVariable UUID id) {
        return quizzes.publish(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize(TEACHER_ROLES)
    public QuizResponse close(@PathVariable UUID id) {
        return quizzes.close(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize(TEACHER_ROLES)
    public QuizResponse get(@PathVariable UUID id) {
        return quizzes.get(id);
    }

    @GetMapping
    @PreAuthorize(TEACHER_ROLES)
    public List<QuizResponse> listByTurma(@RequestParam UUID turmaId) {
        return quizzes.listByTurma(turmaId);
    }

    @GetMapping("/{id}/attempts")
    @PreAuthorize(TEACHER_ROLES)
    public List<QuizAttemptRow> attempts(@PathVariable UUID id) {
        return attempts.attemptsForQuiz(id);
    }

    @PostMapping("/attempts/{attemptId}/grade")
    @PreAuthorize(TEACHER_ROLES)
    public void gradeEssay(@PathVariable UUID attemptId, @Valid @RequestBody ManualGradeBatch batch) {
        attempts.gradeEssayAnswers(attemptId, batch);
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize(TEACHER_ROLES)
    public QuizAnalytics analytics(@PathVariable UUID id) {
        return attempts.analyticsFor(id);
    }
}
