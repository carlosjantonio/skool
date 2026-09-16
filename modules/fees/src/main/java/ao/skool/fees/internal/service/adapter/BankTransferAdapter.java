package ao.skool.fees.internal.service.adapter;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.internal.domain.Invoice;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Bank transfer skeleton. Real reconciliation will read CSV statements from BAI /
 * BFA / BIC / Standard Bank Angola via a scheduled importer (Phase 7 work). For
 * now the adapter records the payment as pending and surfaces the IBAN + reference
 * the guardian should quote on their transfer.
 */
@Component
public class BankTransferAdapter implements PaymentAdapter {

    @Override
    public PaymentMethod method() { return PaymentMethod.BANK_TRANSFER; }

    @Override
    public InitiationResult initiate(Invoice invoice, BigDecimal amount, String payerPhone) {
        // The real IBAN would come from tenant config once the school pilot IDs a bank.
        // Reference is the invoice reference so the CSV importer can match on it.
        String instructions = "Transferência bancária — IBAN: AO06.0006.0000.1234.5678.9012.3 (BAI). "
                + "Referência (obrigatória): " + invoice.reference() + ". Valor: "
                + amount.toPlainString() + " Kz. Envie o comprovativo à secretaria.";
        return new InitiationResult(invoice.reference(), instructions, false);
    }
}
