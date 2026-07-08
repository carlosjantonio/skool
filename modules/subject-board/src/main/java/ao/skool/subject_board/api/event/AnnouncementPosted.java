package ao.skool.subject_board.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementPosted(
        UUID eventId,
        TenantId tenantId,
        UUID entryId,
        UUID subjectId,
        UUID turmaId,
        String title,
        Instant occurredAt
) implements DomainEvent {}
