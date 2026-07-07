package ao.skool.sis.internal.web.dto;

import ao.skool.sis.internal.domain.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class EnrollmentDtos {

    public record CreateEnrollment(
            @NotNull UUID studentId,
            @NotNull UUID academicYearId,
            @NotNull UUID turmaId
    ) {}

    public record EnrollmentResponse(
            UUID id,
            UUID studentId,
            UUID academicYearId,
            UUID turmaId,
            EnrollmentStatus status,
            Instant enrolledAt,
            Instant withdrawnAt
    ) {}

    private EnrollmentDtos() {}
}
