package ao.skool.academic_structure.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class SchoolDtos {

    public record CreateSchool(
            @NotBlank @Size(max = 128) String name,
            @NotBlank @Size(max = 32) String code,
            @NotNull UUID municipioId,
            String comunaOuBairro,
            String addressLine1,
            String addressComplement
    ) {}

    public record SchoolResponse(
            UUID id,
            String name,
            String code,
            UUID municipioId,
            String comunaOuBairro,
            String addressLine1,
            String addressComplement,
            boolean active
    ) {}

    private SchoolDtos() {}
}
