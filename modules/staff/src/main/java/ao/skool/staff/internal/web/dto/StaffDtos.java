package ao.skool.staff.internal.web.dto;

import ao.skool.staff.internal.domain.AssignmentRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class StaffDtos {

    public record CreateStaff(
            @NotBlank @Size(max = 255) String fullName,
            @Size(max = 14) String bi,
            @Size(max = 10) String nif,
            @Size(max = 32) String phone,
            @Email @Size(max = 255) String email,
            @Size(max = 255) String qualification,
            LocalDate hireDate,
            boolean provisionPortalUser
    ) {}

    public record StaffResponse(
            UUID id,
            String fullName,
            String bi,
            String nif,
            String phone,
            String email,
            String qualification,
            LocalDate hireDate,
            UUID userId,
            boolean active
    ) {}

    public record CreateAssignment(
            @NotNull UUID staffId,
            @NotNull UUID turmaId,
            UUID subjectId, // nullable — head teacher assignments omit it
            @NotNull UUID academicYearId,
            @NotNull AssignmentRole role
    ) {}

    public record AssignmentResponse(
            UUID id,
            UUID staffId,
            UUID turmaId,
            UUID subjectId,
            UUID academicYearId,
            AssignmentRole role
    ) {}

    private StaffDtos() {}
}
