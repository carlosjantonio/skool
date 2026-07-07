package ao.skool.sis.internal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class GuardianDtos {

    public record CreateGuardian(
            @NotBlank @Size(max = 255) String fullName,
            @NotBlank @Size(max = 32) String phone,
            @Email String email,
            @Size(max = 14) String bi,
            /** When true and email is present, a portal login is provisioned. */
            boolean provisionPortalUser
    ) {}

    public record GuardianResponse(
            UUID id,
            String fullName,
            String phone,
            String email,
            String bi,
            UUID userId
    ) {}

    private GuardianDtos() {}
}
