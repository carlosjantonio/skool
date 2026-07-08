package ao.skool.assessment.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record QuizPublished(
        UUID eventId,
        TenantId tenantId,
        UUID quizId,
        UUID subjectId,
        UUID turmaId,
        String title,
        Instant occurredAt
) implements DomainEvent {}
