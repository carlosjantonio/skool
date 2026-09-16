package ao.skool.fees.internal.persistence;

import ao.skool.fees.internal.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByInvoiceIdOrderByReceivedAtAsc(UUID invoiceId);
}
