package ao.skool.fees.internal.service;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.security.audit.AuditLogWriter;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.fees.api.event.InvoiceOverdue;
import ao.skool.fees.api.event.PaymentReceived;
import ao.skool.fees.internal.domain.Invoice;
import ao.skool.fees.internal.domain.InvoiceStatus;
import ao.skool.fees.internal.domain.Payment;
import ao.skool.fees.internal.persistence.InvoiceRepository;
import ao.skool.fees.internal.persistence.PaymentRepository;
import ao.skool.fees.internal.service.adapter.PaymentAdapter;
import ao.skool.fees.internal.service.adapter.PaymentAdapterRegistry;
import ao.skool.fees.internal.web.dto.FeeDtos.CreatePayment;
import ao.skool.fees.internal.web.dto.FeeDtos.DefaulterRow;
import ao.skool.fees.internal.web.dto.FeeDtos.InitiatePayment;
import ao.skool.fees.internal.web.dto.FeeDtos.PaymentInitiation;
import ao.skool.fees.internal.web.dto.FeeDtos.PaymentResponse;
import ao.skool.sis.api.StudentDirectory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final PaymentAdapterRegistry adapters;
    private final StudentDirectory studentDirectory;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;
    private final AuditLogWriter audit;

    public PaymentService(InvoiceRepository invoices, PaymentRepository payments,
                          PaymentAdapterRegistry adapters, StudentDirectory studentDirectory,
                          TenantContext tenant, ApplicationEventPublisher events,
                          AuditLogWriter audit) {
        this.invoices = invoices;
        this.payments = payments;
        this.adapters = adapters;
        this.studentDirectory = studentDirectory;
        this.tenant = tenant;
        this.events = events;
        this.audit = audit;
    }

    /**
     * Record a payment already collected out-of-band (cash, POS receipt, reconciled
     * bank line). This is what the admin uses. For adapter-driven flows call
     * {@link #initiate} first, then {@link #record} once the vendor confirms.
     */
    public PaymentResponse record(CreatePayment cmd) {
        Invoice invoice = invoices.findById(cmd.invoiceId()).orElseThrow(NotFoundException::new);
        if (!invoice.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        if (invoice.status() == InvoiceStatus.CANCELLED) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }
        if (cmd.amount().compareTo(invoice.outstanding()) > 0) {
            // Overpayment refused — the school can split it into two payments if needed.
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
        }

        Payment p = new Payment(UUID.randomUUID(), invoice.tenantId(), invoice.id(),
                cmd.amount(), cmd.method(), cmd.externalReference(), currentActor(), cmd.notes());
        payments.save(p);
        boolean fullyPaid = invoice.applyPayment(cmd.amount());
        audit.record("payment.record", "invoice", invoice.id().toString());
        events.publishEvent(new PaymentReceived(UUID.randomUUID(), tenant.current(),
                p.id(), invoice.id(), invoice.studentId(), cmd.amount(),
                cmd.method(), fullyPaid, Instant.now()));
        return toPayment(p);
    }

    /**
     * Start a payment via a vendor adapter. Returns the vendor reference the payer
     * takes to the ATM / USSD. When the vendor confirms (webhook or CSV recon) the
     * admin / scheduler calls {@link #record} to close the loop.
     */
    public PaymentInitiation initiate(InitiatePayment cmd) {
        Invoice invoice = invoices.findById(cmd.invoiceId()).orElseThrow(NotFoundException::new);
        if (!invoice.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        if (invoice.status() == InvoiceStatus.PAID || invoice.status() == InvoiceStatus.CANCELLED) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }
        PaymentAdapter adapter = adapters.forMethod(cmd.method());
        var result = adapter.initiate(invoice, invoice.outstanding(), cmd.payerPhone());
        // Skeleton adapters that settle immediately (STUB_MANUAL) go straight to record.
        if (result.settledImmediately()) {
            record(new CreatePayment(invoice.id(), invoice.outstanding(), cmd.method(),
                    result.vendorReference(), "Auto-recorded via " + cmd.method()));
        }
        return new PaymentInitiation(result.vendorReference(), result.instructions(), cmd.method());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPaymentsForInvoice(UUID invoiceId) {
        Invoice invoice = invoices.findById(invoiceId).orElseThrow(NotFoundException::new);
        if (!invoice.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        return payments.findByInvoiceIdOrderByReceivedAtAsc(invoiceId).stream()
                .map(this::toPayment).toList();
    }

    /**
     * Defaulter report: aggregated rows of students who owe money on overdue or
     * partially-paid invoices. Sorted by outstanding total desc so the biggest
     * debtors surface first.
     */
    @Transactional(readOnly = true)
    public List<DefaulterRow> defaulters() {
        UUID tenantId = tenant.current().value();
        List<InvoiceStatus> chase = List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE);
        List<Invoice> overdue = invoices.findByTenantIdAndDueAtBeforeAndStatusIn(
                tenantId, Instant.now(), chase);

        // Aggregate by student.
        Map<UUID, DefaulterRow> byStudent = new HashMap<>();
        Map<UUID, String> nameCache = new HashMap<>();
        for (Invoice i : overdue) {
            String name = nameCache.computeIfAbsent(i.studentId(),
                    sid -> studentDirectory.findStudent(sid)
                            .map(StudentDirectory.StudentSummary::fullName).orElse("?"));
            var existing = byStudent.get(i.studentId());
            BigDecimal outstanding = i.outstanding();
            if (existing == null) {
                byStudent.put(i.studentId(), new DefaulterRow(i.studentId(), name, 1, outstanding, i.dueAt()));
            } else {
                Instant oldest = i.dueAt().isBefore(existing.oldestDueAt()) ? i.dueAt() : existing.oldestDueAt();
                byStudent.put(i.studentId(), new DefaulterRow(
                        i.studentId(), name,
                        existing.overdueInvoiceCount() + 1,
                        existing.totalOutstanding().add(outstanding),
                        oldest));
            }
        }
        List<DefaulterRow> out = new ArrayList<>(byStudent.values());
        out.sort((a, b) -> b.totalOutstanding().compareTo(a.totalOutstanding()));
        return out;
    }

    /**
     * Sweep every past-due invoice, flip status to OVERDUE, publish an event. Runs
     * on a schedule (hourly is fine; per-invoice dedup is via the status guard).
     * Also invocable directly for tests / demo.
     */
    public int sweepOverdue() {
        UUID tenantId = tenant.current().value();
        List<InvoiceStatus> chase = List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL);
        Instant now = Instant.now();
        List<Invoice> due = invoices.findByTenantIdAndDueAtBeforeAndStatusIn(tenantId, now, chase);
        int flipped = 0;
        for (Invoice i : due) {
            InvoiceStatus before = i.status();
            i.markOverdue();
            if (i.status() != before) {
                long days = Math.max(0, Duration.between(i.dueAt(), now).toDays());
                events.publishEvent(new InvoiceOverdue(UUID.randomUUID(), tenant.current(),
                        i.id(), i.studentId(), i.reference(), i.outstanding(),
                        i.dueAt(), days, Instant.now()));
                flipped++;
            }
        }
        return flipped;
    }

    private PaymentResponse toPayment(Payment p) {
        return new PaymentResponse(p.id(), p.invoiceId(), p.amount(), p.currency(),
                p.method(), p.externalReference(), p.receivedAt(), p.notes());
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            try { return UUID.fromString(p.userId()); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
}
