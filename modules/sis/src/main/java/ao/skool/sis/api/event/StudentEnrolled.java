package ao.skool.sis.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StudentEnrolled(
        UUID eventId,
        TenantId tenantId,
        UUID studentId,
        UUID academicYearId,
        UUID turmaId,
        Instant occurredAt
) implements DomainEvent {}
