package ao.skool.fees.internal.web.dto;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.internal.domain.FeeKind;
import ao.skool.fees.internal.domain.InvoiceStatus;
import ao.skool.fees.internal.domain.ScholarshipKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FeeDtos {

    public record CreateFeeSchedule(
            @NotNull UUID academicYearId,
            @NotBlank String name,
            @NotNull FeeKind kind,
            @NotNull @DecimalMin("0.00") BigDecimal amount,
            String gradeLevel,
            @Pattern(regexp = "T[1-3]") String trimesterKey,
            @Min(1) @Max(12) Integer periodMonth,
            @NotNull Instant dueAt
    ) {}

    public record FeeScheduleResponse(
            UUID id,
            UUID academicYearId,
            String name,
            FeeKind kind,
            BigDecimal amount,
            String currency,
            String gradeLevel,
            String trimesterKey,
            Integer periodMonth,
            Instant dueAt,
            boolean active,
            Instant createdAt
    ) {}

    public record CreateScholarship(
            @NotNull UUID studentId,
            @NotNull ScholarshipKind kind,
            @DecimalMin("0.00") BigDecimal percentage,
            @DecimalMin("0.00") BigDecimal fixedAmount,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            String reason
    ) {}

    public record ScholarshipResponse(
            UUID id,
            UUID studentId,
            ScholarshipKind kind,
            BigDecimal percentage,
            BigDecimal fixedAmount,
            LocalDate validFrom,
            LocalDate validTo,
            String reason,
            boolean active
    ) {}

    public record InvoiceResponse(
            UUID id,
            UUID studentId,
            UUID feeScheduleId,
            String reference,
            String title,
            BigDecimal amountGross,
            BigDecimal amountDiscount,
            BigDecimal amountNet,
            BigDecimal amountPaid,
            BigDecimal outstanding,
            String currency,
            Instant issuedAt,
            Instant dueAt,
            InvoiceStatus status,
            String studentName
    ) {}

    public record BillingResult(
            int issuedCount,
            int skippedCount,
            int failedCount,
            java.util.List<InvoiceResponse> issued
    ) {}

    public record CreatePayment(
            @NotNull UUID invoiceId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull PaymentMethod method,
            String externalReference,
            String notes
    ) {}

    public record PaymentResponse(
            UUID id,
            UUID invoiceId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            String externalReference,
            Instant receivedAt,
            String notes
    ) {}

    public record DefaulterRow(
            UUID studentId,
            String studentName,
            int overdueInvoiceCount,
            BigDecimal totalOutstanding,
            Instant oldestDueAt
    ) {}

    /** Response returned when initiating a mobile-money or bank charge. */
    public record PaymentInitiation(
            String vendorReference,
            String instructions,
            PaymentMethod method
    ) {}

    public record InitiatePayment(
            @NotNull UUID invoiceId,
            @NotNull PaymentMethod method,
            String payerPhone
    ) {}

    private FeeDtos() {}
}
