package ao.skool.fees.internal.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices",
       uniqueConstraints = @UniqueConstraint(name = "uk_invoice_schedule_student",
                                              columnNames = {"fee_schedule_id", "student_id"}),
       indexes = {
           @Index(name = "idx_invoices_tenant_student", columnList = "tenant_id, student_id"),
           @Index(name = "idx_invoices_due", columnList = "due_at"),
           @Index(name = "idx_invoices_status", columnList = "status")
       })
public class Invoice {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "fee_schedule_id", nullable = false)
    private UUID feeScheduleId;

    /** Human-facing invoice number, e.g. {@code INV-20260701-3F2A}. Unique per tenant. */
    @Column(nullable = false, unique = true, length = 32)
    private String reference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "amount_gross", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountGross;

    @Column(name = "amount_discount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountDiscount = BigDecimal.ZERO;

    @Column(name = "amount_net", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountNet;

    @Column(name = "amount_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "AOA";

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(name = "scholarship_id")
    private UUID scholarshipId;

    protected Invoice() {}

    public Invoice(UUID id, UUID tenantId, UUID studentId, UUID feeScheduleId, String reference,
                   String title, BigDecimal amountGross, BigDecimal amountDiscount,
                   Instant dueAt, UUID scholarshipId) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.feeScheduleId = feeScheduleId;
        this.reference = reference;
        this.title = title;
        this.amountGross = amountGross;
        this.amountDiscount = amountDiscount == null ? BigDecimal.ZERO : amountDiscount;
        this.amountNet = amountGross.subtract(this.amountDiscount);
        this.dueAt = dueAt;
        this.scholarshipId = scholarshipId;
    }

    /** Applies a payment; recomputes status. Returns true if fully paid. */
    public boolean applyPayment(BigDecimal amount) {
        this.amountPaid = this.amountPaid.add(amount);
        if (amountPaid.compareTo(amountNet) >= 0) {
            this.status = InvoiceStatus.PAID;
            return true;
        }
        this.status = InvoiceStatus.PARTIAL;
        return false;
    }

    public void markOverdue() {
        // Don't override terminal states.
        if (status == InvoiceStatus.ISSUED || status == InvoiceStatus.PARTIAL) {
            this.status = InvoiceStatus.OVERDUE;
        }
    }

    public void cancel() {
        this.status = InvoiceStatus.CANCELLED;
    }

    public BigDecimal outstanding() {
        return amountNet.subtract(amountPaid);
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID studentId() { return studentId; }
    public UUID feeScheduleId() { return feeScheduleId; }
    public String reference() { return reference; }
    public String title() { return title; }
    public BigDecimal amountGross() { return amountGross; }
    public BigDecimal amountDiscount() { return amountDiscount; }
    public BigDecimal amountNet() { return amountNet; }
    public BigDecimal amountPaid() { return amountPaid; }
    public String currency() { return currency; }
    public Instant issuedAt() { return issuedAt; }
    public Instant dueAt() { return dueAt; }
    public InvoiceStatus status() { return status; }
    public UUID scholarshipId() { return scholarshipId; }
}
