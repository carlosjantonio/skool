package ao.skool.fees.internal.service.adapter;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.internal.domain.Invoice;

import java.math.BigDecimal;

/**
 * Strategy interface every payment method plugs into. The service layer looks up
 * the adapter by {@link PaymentMethod} and calls {@link #initiate} — the adapter
 * either records the payment immediately (stub / manual bank reconciliation) or
 * returns a vendor reference the payer takes to the gateway.
 * <p>
 * Implementations must be Spring beans; the {@link PaymentAdapterRegistry} builds
 * a method → adapter map at startup.
 */
public interface PaymentAdapter {

    PaymentMethod method();

    /**
     * Begin a payment for {@code invoice} of {@code amount}. May return a synchronous
     * result (stub) or a pending reference (mobile money). Vendor call failures are
     * surfaced via {@link PaymentAdapterException}.
     *
     * @param payerPhone optional MSISDN for mobile-money methods
     */
    InitiationResult initiate(Invoice invoice, BigDecimal amount, String payerPhone);

    record InitiationResult(
            /** Vendor-side reference token or receipt id. */
            String vendorReference,
            /** Human-readable next step in pt-AO (e.g. USSD code to dial). */
            String instructions,
            /** True when the money has been captured now; false when it's pending. */
            boolean settledImmediately
    ) {}

    class PaymentAdapterException extends RuntimeException {
        public PaymentAdapterException(String message) { super(message); }
        public PaymentAdapterException(String message, Throwable cause) { super(message, cause); }
    }
}
