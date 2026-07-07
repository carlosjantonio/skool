package ao.skool.academic_structure.internal.web.dto;

import ao.skool.academic_structure.internal.domain.GradeLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class SubjectDtos {

    public record CreateSubject(
            @NotBlank @Size(max = 128) String name,
            @NotBlank @Size(max = 32) String code,
            @NotNull GradeLevel gradeLevel
    ) {}

    public record SubjectResponse(UUID id, String name, String code, GradeLevel gradeLevel) {}

    private SubjectDtos() {}
}
