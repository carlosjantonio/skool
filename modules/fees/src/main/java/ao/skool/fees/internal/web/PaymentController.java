package ao.skool.fees.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.fees.internal.service.PaymentService;
import ao.skool.fees.internal.web.dto.FeeDtos.CreatePayment;
import ao.skool.fees.internal.web.dto.FeeDtos.DefaulterRow;
import ao.skool.fees.internal.web.dto.FeeDtos.InitiatePayment;
import ao.skool.fees.internal.web.dto.FeeDtos.PaymentInitiation;
import ao.skool.fees.internal.web.dto.FeeDtos.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final String ADMIN_ROLES = "hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')";

    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    @PreAuthorize(ADMIN_ROLES)
    public PaymentResponse record(@Valid @RequestBody CreatePayment cmd) {
        return service.record(cmd);
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.GUARDIAN + "')")
    public PaymentInitiation initiate(@Valid @RequestBody InitiatePayment cmd) {
        return service.initiate(cmd);
    }

    @GetMapping
    @PreAuthorize(ADMIN_ROLES)
    public List<PaymentResponse> forInvoice(@RequestParam UUID invoiceId) {
        return service.listPaymentsForInvoice(invoiceId);
    }

    @GetMapping("/defaulters")
    @PreAuthorize(ADMIN_ROLES)
    public List<DefaulterRow> defaulters() {
        return service.defaulters();
    }

    @PostMapping("/sweep-overdue")
    @PreAuthorize(ADMIN_ROLES)
    public Map<String, Integer> sweepOverdue() {
        return Map.of("flipped", service.sweepOverdue());
    }
}
