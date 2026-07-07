package ao.skool.academic_structure.internal.web.dto;

import ao.skool.academic_structure.internal.domain.CurricularTrack;
import ao.skool.academic_structure.internal.domain.GradeLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class TurmaDtos {

    public record CreateTurma(
            @NotNull UUID schoolId,
            @NotNull UUID academicYearId,
            @NotBlank @Size(max = 32) String name,
            @NotNull GradeLevel gradeLevel,
            @NotNull CurricularTrack track,
            @Min(1) int capacity
    ) {}

    public record TurmaResponse(
            UUID id,
            UUID schoolId,
            UUID academicYearId,
            String name,
            GradeLevel gradeLevel,
            CurricularTrack track,
            int capacity
    ) {}

    private TurmaDtos() {}
}
