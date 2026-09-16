package ao.skool.fees.internal.service.adapter;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.internal.domain.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AfricellMoneyAdapter implements PaymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(AfricellMoneyAdapter.class);

    @Override
    public PaymentMethod method() { return PaymentMethod.AFRICELL_MONEY; }

    @Override
    public InitiationResult initiate(Invoice invoice, BigDecimal amount, String payerPhone) {
        String ref = "AFR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Africell Money: would issue reference {} for invoice {} amount {} AOA (payer {})",
                ref, invoice.reference(), amount, payerPhone);
        String instructions = "Digite *444# na app Africell Money e selecione 'Pagar'. "
                + "Referência " + ref + " · Valor " + amount.toPlainString() + " Kz.";
        return new InitiationResult(ref, instructions, false);
    }
}
