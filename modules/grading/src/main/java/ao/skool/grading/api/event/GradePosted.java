package ao.skool.grading.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GradePosted(
        UUID eventId,
        TenantId tenantId,
        UUID gradeId,
        UUID studentId,
        UUID subjectId,
        String trimesterKey,
        BigDecimal value,
        Instant occurredAt
) implements DomainEvent {}
