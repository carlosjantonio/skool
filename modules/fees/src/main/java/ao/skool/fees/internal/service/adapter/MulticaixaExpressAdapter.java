package ao.skool.fees.internal.service.adapter;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.internal.domain.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Multicaixa Express reference-payment skeleton. Real integration is gated on
 * procurement (see INITIAL_PLAN.md §7 open questions) — for now we generate a
 * reference number in the standard EMIS format and log the "would call vendor"
 * intent so the guardian portal can render the exact digits to type into the ATM.
 */
@Component
public class MulticaixaExpressAdapter implements PaymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(MulticaixaExpressAdapter.class);

    @Override
    public PaymentMethod method() { return PaymentMethod.MULTICAIXA_EXPRESS; }

    @Override
    public InitiationResult initiate(Invoice invoice, BigDecimal amount, String payerPhone) {
        // Real integration would POST to EMIS gateway here and receive a reference.
        // The 9-digit reference shape matches what ATMs display for reference payments.
        String ref = "MCX-" + numeric9();
        log.info("Multicaixa Express: would issue reference {} for invoice {} amount {} AOA",
                ref, invoice.reference(), amount);
        String instructions = "Multicaixa Express: Pagamentos → Pagamento por Referência → Entidade 11333, "
                + "Referência " + ref + ", Valor " + amount.toPlainString() + " Kz.";
        return new InitiationResult(ref, instructions, false);
    }

    private String numeric9() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        StringBuilder digits = new StringBuilder();
        for (char c : uuid.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c);
            if (digits.length() == 9) break;
        }
        while (digits.length() < 9) digits.append('0');
        return digits.toString();
    }
}
