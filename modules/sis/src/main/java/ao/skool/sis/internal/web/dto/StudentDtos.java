package ao.skool.sis.internal.web.dto;

import ao.skool.sis.internal.domain.GuardianRelationship;
import ao.skool.sis.internal.domain.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StudentDtos {

    public record CreateStudent(
            @NotBlank @Size(max = 255) String fullName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Sex sex,
            @Size(max = 14) String bi,
            String comunaOuBairro,
            String addressLine1,
            String healthNotes
    ) {}

    public record LinkGuardian(
            @NotNull UUID guardianId,
            @NotNull GuardianRelationship relationship,
            boolean primaryGuardian,
            boolean emergencyContact
    ) {}

    public record GuardianSummary(
            UUID id,
            String fullName,
            String phone,
            String email,
            GuardianRelationship relationship,
            boolean primaryGuardian
    ) {}

    public record StudentResponse(
            UUID id,
            String fullName,
            LocalDate dateOfBirth,
            Sex sex,
            String bi,
            String comunaOuBairro,
            String addressLine1,
            String healthNotes,
            boolean active,
            List<GuardianSummary> guardians
    ) {}

    private StudentDtos() {}
}
