package ao.skool.fees.api.event;

import ao.skool.common.domain.TenantId;
import ao.skool.common.domain.event.DomainEvent;
import ao.skool.fees.api.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceived(
        UUID eventId,
        TenantId tenantId,
        UUID paymentId,
        UUID invoiceId,
        UUID studentId,
        BigDecimal amount,
        PaymentMethod method,
        boolean invoiceFullyPaid,
        Instant occurredAt
) implements DomainEvent {}
