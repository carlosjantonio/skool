package ao.skool.fees.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceOverdue(
        UUID eventId,
        TenantId tenantId,
        UUID invoiceId,
        UUID studentId,
        String reference,
        BigDecimal outstanding,
        Instant dueAt,
        long daysPastDue,
        Instant occurredAt
) implements DomainEvent {}
