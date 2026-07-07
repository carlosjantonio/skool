package ao.skool.attendance.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceMarkedAbsent(
        UUID eventId,
        TenantId tenantId,
        UUID studentId,
        UUID turmaId,
        LocalDate date,
        Instant occurredAt
) implements DomainEvent {}
