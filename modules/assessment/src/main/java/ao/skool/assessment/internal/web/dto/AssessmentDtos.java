package ao.skool.assessment.internal.web.dto;

import ao.skool.assessment.internal.domain.AttemptStatus;
import ao.skool.assessment.internal.domain.QuestionType;
import ao.skool.assessment.internal.domain.QuizStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AssessmentDtos {

    public record CreateQuestion(
            @NotNull UUID subjectId,
            @NotBlank String gradeLevel,
            @NotBlank String prompt,
            @NotNull QuestionType questionType,
            @NotNull JsonNode payload,
            BigDecimal points
    ) {}

    public record QuestionResponse(
            UUID id,
            UUID subjectId,
            String gradeLevel,
            String prompt,
            QuestionType questionType,
            JsonNode payload,
            BigDecimal points,
            Instant createdAt
    ) {}

    /** Question shape sent to students — never includes the correct answer / rubric. */
    public record StudentQuestion(
            UUID id,
            String prompt,
            QuestionType questionType,
            JsonNode payload,
            BigDecimal points
    ) {}

    public record CreateQuiz(
            @NotNull UUID subjectId,
            @NotNull UUID turmaId,
            @NotNull UUID academicYearId,
            @NotBlank @Pattern(regexp = "T[1-3]") String trimesterKey,
            @NotBlank String title,
            String instructions,
            @Positive Integer timeLimitSeconds,
            Boolean randomizeQuestions,
            Boolean randomizeOptions,
            Instant opensAt,
            Instant closesAt,
            @NotEmpty List<UUID> questionIds
    ) {}

    public record QuizResponse(
            UUID id,
            UUID subjectId,
            UUID turmaId,
            UUID academicYearId,
            String trimesterKey,
            String title,
            String instructions,
            Integer timeLimitSeconds,
            boolean randomizeQuestions,
            boolean randomizeOptions,
            QuizStatus status,
            Instant opensAt,
            Instant closesAt,
            Instant publishedAt,
            int questionCount
    ) {}

    /** Payload sent to a student when they start (or resume) an attempt. */
    public record AttemptView(
            UUID attemptId,
            UUID quizId,
            String title,
            String instructions,
            Integer timeLimitSeconds,
            Instant startedAt,
            Instant deadlineAt,
            AttemptStatus status,
            List<StudentQuestion> questions,
            List<SavedAnswer> savedAnswers
    ) {}

    public record SavedAnswer(UUID questionId, JsonNode response) {}

    public record SubmitAnswer(
            @NotNull UUID id,           // client-generated so retries are idempotent
            @NotNull UUID questionId,
            @NotNull JsonNode response
    ) {}

    public record SubmitAttempt(
            @Valid @NotNull List<SubmitAnswer> answers,
            Integer tabSwitchCount
    ) {}

    public record AttemptResult(
            UUID attemptId,
            AttemptStatus status,
            BigDecimal autoScore,
            BigDecimal manualScore,
            BigDecimal totalPoints,
            boolean needsManualGrading
    ) {}

    public record QuizAttemptRow(
            UUID attemptId,
            UUID studentId,
            AttemptStatus status,
            Instant startedAt,
            Instant submittedAt,
            BigDecimal autoScore,
            BigDecimal manualScore,
            BigDecimal totalPoints,
            int tabSwitchCount
    ) {}

    public record ManualGradeCommand(
            @NotNull UUID answerId,
            @NotNull BigDecimal pointsEarned,
            String feedback
    ) {}

    public record ManualGradeBatch(@Valid @NotEmpty List<ManualGradeCommand> grades) {}

    public record QuizAnalytics(
            UUID quizId,
            int totalAttempts,
            int submittedAttempts,
            BigDecimal averageAutoScore,
            BigDecimal averageTotalPoints,
            List<QuestionStat> questionStats
    ) {}

    public record QuestionStat(
            UUID questionId,
            String prompt,
            int answeredCount,
            int correctCount,
            BigDecimal correctRate
    ) {}

    private AssessmentDtos() {}
}
