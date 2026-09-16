package ao.skool.fees.internal.persistence;

import ao.skool.fees.internal.domain.Invoice;
import ao.skool.fees.internal.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByStudentIdOrderByIssuedAtDesc(UUID studentId);

    List<Invoice> findByTenantIdOrderByIssuedAtDesc(UUID tenantId);

    List<Invoice> findByTenantIdAndStatusInOrderByDueAtAsc(UUID tenantId, List<InvoiceStatus> statuses);

    boolean existsByFeeScheduleIdAndStudentId(UUID feeScheduleId, UUID studentId);

    /** Snapshot of invoices whose due date has passed and still owe money. */
    List<Invoice> findByTenantIdAndDueAtBeforeAndStatusIn(UUID tenantId, Instant now, List<InvoiceStatus> statuses);
}
