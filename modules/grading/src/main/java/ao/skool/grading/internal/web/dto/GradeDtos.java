package ao.skool.grading.internal.web.dto;

import ao.skool.grading.internal.domain.GradeCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GradeDtos {

    public record CreateGrade(
            @NotNull UUID studentId,
            @NotNull UUID subjectId,
            @NotNull UUID turmaId,
            @NotNull UUID academicYearId,
            @NotBlank @Pattern(regexp = "T[1-3]") String trimesterKey,
            @NotNull @DecimalMin("0.00") @DecimalMax("20.00") BigDecimal value,
            BigDecimal weight,
            @NotNull GradeCategory category,
            String notes
    ) {}

    public record BatchGrades(@Valid @NotEmpty List<CreateGrade> grades) {}

    public record GradeResponse(
            UUID id,
            UUID studentId,
            UUID subjectId,
            UUID turmaId,
            String trimesterKey,
            BigDecimal value,
            BigDecimal weight,
            GradeCategory category,
            String notes,
            Instant recordedAt
    ) {}

    public record TrimesterSubjectAverage(
            String trimesterKey,
            UUID subjectId,
            String subjectName,
            BigDecimal average
    ) {}

    public record StudentGradeSummary(
            UUID studentId,
            String studentName,
            UUID academicYearId,
            String academicYearName,
            List<TrimesterSubjectAverage> subjectAverages,
            Map<String, BigDecimal> trimesterAverages,
            BigDecimal finalAverage
    ) {}

    private GradeDtos() {}
}
