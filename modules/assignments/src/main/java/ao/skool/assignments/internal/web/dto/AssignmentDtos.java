package ao.skool.assignments.internal.web.dto;

import ao.skool.assignments.internal.domain.AssignmentStatus;
import ao.skool.assignments.internal.domain.SubmissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AssignmentDtos {

    public record CreateAssignment(
            @NotNull UUID subjectId,
            @NotNull UUID turmaId,
            @NotNull UUID academicYearId,
            @NotBlank @Pattern(regexp = "T[1-3]") String trimesterKey,
            @NotBlank String title,
            String description,
            String rubric,
            @Positive BigDecimal maxScore,
            @NotNull Instant dueAt,
            Boolean allowLate
    ) {}

    public record AssignmentResponse(
            UUID id,
            UUID subjectId,
            UUID turmaId,
            UUID academicYearId,
            String trimesterKey,
            String title,
            String description,
            String rubric,
            BigDecimal maxScore,
            Instant dueAt,
            boolean allowLate,
            AssignmentStatus status,
            Instant createdAt,
            int submissionCount
    ) {}

    public record SubmitAssignment(
            UUID documentId,   // optional — student may hand-write in the notes field only
            String notes
    ) {}

    public record SubmissionResponse(
            UUID id,
            UUID assignmentId,
            UUID studentId,
            UUID documentId,
            String notes,
            Instant submittedAt,
            boolean isLate,
            BigDecimal score,
            String feedback,
            Instant gradedAt,
            SubmissionStatus status,
            String studentName
    ) {}

    public record GradeSubmission(
            @NotNull BigDecimal score,
            String feedback
    ) {}

    public record StudentAssignmentView(
            AssignmentResponse assignment,
            SubmissionResponse mySubmission,
            String subjectName
    ) {}

    public record TeacherAssignmentDetail(
            AssignmentResponse assignment,
            List<SubmissionResponse> submissions
    ) {}

    private AssignmentDtos() {}
}
