package ao.skool.fees.api;

/**
 * Kept in the public {@code api} package because other modules (notifications, reporting)
 * will reference the method when consuming {@code PaymentReceived} events.
 */
public enum PaymentMethod {
    /** Admin marks the invoice paid manually — no external gateway hit. */
    STUB_MANUAL,
    MULTICAIXA_EXPRESS,
    UNITEL_MONEY,
    AFRICELL_MONEY,
    /** Reconciled from a bank statement (BAI / BFA / BIC / Standard Bank Angola CSV). */
    BANK_TRANSFER
}
