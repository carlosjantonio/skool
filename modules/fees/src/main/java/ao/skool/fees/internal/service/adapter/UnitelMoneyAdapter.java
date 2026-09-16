package ao.skool.fees.internal.service.adapter;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.internal.domain.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Unitel Money USSD skeleton. Real vendor integration TBD; for now the adapter
 * returns the USSD string the payer types into their phone plus a pending
 * reference. Reconciliation would arrive via a Unitel Money callback endpoint.
 */
@Component
public class UnitelMoneyAdapter implements PaymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(UnitelMoneyAdapter.class);

    @Override
    public PaymentMethod method() { return PaymentMethod.UNITEL_MONEY; }

    @Override
    public InitiationResult initiate(Invoice invoice, BigDecimal amount, String payerPhone) {
        String ref = "UMN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Unitel Money: would issue reference {} for invoice {} amount {} AOA (payer {})",
                ref, invoice.reference(), amount, payerPhone);
        String instructions = "Digite *400# no Unitel Money e escolha 'Pagar a merchant'. "
                + "Referência " + ref + " · Valor " + amount.toPlainString() + " Kz.";
        return new InitiationResult(ref, instructions, false);
    }
}
