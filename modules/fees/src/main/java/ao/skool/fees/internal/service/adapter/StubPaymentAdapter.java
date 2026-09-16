package ao.skool.fees.internal.service.adapter;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.internal.domain.Invoice;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Manual-mark adapter: the admin has already collected cash / a Multicaixa receipt
 * off-band and just wants to record it against the invoice. There's no external
 * call — {@link #initiate} settles immediately.
 */
@Component
public class StubPaymentAdapter implements PaymentAdapter {

    @Override
    public PaymentMethod method() { return PaymentMethod.STUB_MANUAL; }

    @Override
    public InitiationResult initiate(Invoice invoice, BigDecimal amount, String payerPhone) {
        String ref = "MANUAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new InitiationResult(ref, "Pagamento registado manualmente.", true);
    }
}
