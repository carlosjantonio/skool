package ao.skool.fees.internal.domain;

import ao.skool.fees.api.PaymentMethod;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments",
       indexes = {
           @Index(name = "idx_payments_tenant_invoice", columnList = "tenant_id, invoice_id"),
           @Index(name = "idx_payments_received", columnList = "received_at")
       })
public class Payment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "AOA";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod method;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected Payment() {}

    public Payment(UUID id, UUID tenantId, UUID invoiceId, BigDecimal amount,
                   PaymentMethod method, String externalReference, UUID receivedBy, String notes) {
        this.id = id;
        this.tenantId = tenantId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.method = method;
        this.externalReference = externalReference;
        this.receivedBy = receivedBy;
        this.notes = notes;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID invoiceId() { return invoiceId; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public PaymentMethod method() { return method; }
    public String externalReference() { return externalReference; }
    public Instant receivedAt() { return receivedAt; }
    public UUID receivedBy() { return receivedBy; }
    public String notes() { return notes; }
}
