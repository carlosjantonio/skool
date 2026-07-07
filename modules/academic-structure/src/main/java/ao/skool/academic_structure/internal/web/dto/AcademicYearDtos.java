package ao.skool.academic_structure.internal.web.dto;

import ao.skool.academic_structure.internal.domain.TrimesterKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AcademicYearDtos {

    public record TrimesterInput(
            @NotNull TrimesterKey key,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record CreateAcademicYear(
            @NotBlank @Size(max = 32) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @Valid List<TrimesterInput> trimesters
    ) {}

    public record TrimesterResponse(UUID id, TrimesterKey key, LocalDate startDate, LocalDate endDate) {}

    public record AcademicYearResponse(
            UUID id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            List<TrimesterResponse> trimesters
    ) {}

    private AcademicYearDtos() {}
}
