package ao.skool.assessment.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuizSubmitted(
        UUID eventId,
        TenantId tenantId,
        UUID attemptId,
        UUID quizId,
        UUID studentId,
        BigDecimal autoScore,
        BigDecimal totalPoints,
        boolean needsManualGrading,
        Instant occurredAt
) implements DomainEvent {}
