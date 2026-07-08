package ao.skool.forum.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ForumReplyPosted(
        UUID eventId,
        TenantId tenantId,
        UUID forumId,
        UUID threadId,
        UUID postId,
        UUID threadAuthorId,
        UUID replyAuthorId,
        String replyAuthorRole,
        Instant occurredAt
) implements DomainEvent {}
