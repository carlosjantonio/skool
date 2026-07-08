package ao.skool.assignments.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AssignmentDue(
        UUID eventId,
        TenantId tenantId,
        UUID assignmentId,
        UUID subjectId,
        UUID turmaId,
        String title,
        Instant dueAt,
        Instant occurredAt
) implements DomainEvent {}
